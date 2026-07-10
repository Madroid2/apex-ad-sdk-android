package com.apexads.sdk.core.data.repository;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.domain.repository.AdRepository;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;
import com.apexads.sdk.core.network.AdNetworkClient;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.request.OpenRTBRequestBuilder;
import com.apexads.sdk.core.tracking.AuctionMacros;
import com.apexads.sdk.core.utils.AdLog;
import com.apexads.sdk.internal.ApexSdkRuntime;

public final class OpenRTBAdRepository implements AdRepository {

    private final AdNetworkClient networkClient;
    private final OpenRTBRequestBuilder requestBuilder;

    public OpenRTBAdRepository(
            @NonNull AdNetworkClient networkClient,
            @NonNull OpenRTBRequestBuilder requestBuilder) {
        this.networkClient = networkClient;
        this.requestBuilder = requestBuilder;
    }

    @Override
    public void loadAd(
            @NonNull AdFormat format,
            @NonNull AdSize size,
            @NonNull String placementId,
            double bidFloor,
            @NonNull OnSuccess onSuccess,
            @NonNull OnFailure onFailure) {

        SdkExecutors.IO.execute(() -> {
            try {
                BidRequest request = requestBuilder
                        .adFormat(format)
                        .adSize(size)
                        .placementId(placementId)
                        .bidFloor(bidFloor)
                        .build();

                BidResponse response = networkClient.requestBid(request);
                BidResponse.Bid bid = response.getWinningBid();

                if (bid == null || bid.adm == null || bid.adm.isEmpty()) {
                    SdkExecutors.MAIN.post(() ->
                            onFailure.onFailure(new AdError.NoFill()));
                    return;
                }

                AdData data = AdData.fromBid(
                        request.id, bid, format,
                        response.cur != null ? response.cur : "USD",
                        ApexSdkRuntime.getConfig().getCacheTtlSeconds());

                fireLossNotices(request.id, response, bid);

                AdLog.i("OpenRTBAdRepository[%s]: loaded cpm=$%.2f crid=%s",
                        placementId, bid.price, bid.crid);
                SdkExecutors.MAIN.post(() -> onSuccess.onSuccess(data));

            } catch (Exception e) {
                AdLog.e(e, "OpenRTBAdRepository[%s]: load failed — %s", placementId, e.getMessage());
                SdkExecutors.MAIN.post(() ->
                        onFailure.onFailure(new AdError.Network(e.getMessage(), e)));
            }
        });
    }

    /**
     * Fires {@code lurl} loss notices for every bid that lost the on-device auction.
     * DSPs tune bidding off loss signals; already on the IO executor here.
     */
    private void fireLossNotices(String requestId, BidResponse response, BidResponse.Bid winner) {
        if (response.seatbid == null) return;
        for (BidResponse.SeatBid sb : response.seatbid) {
            if (sb.bid == null) continue;
            for (BidResponse.Bid b : sb.bid) {
                if (b == winner || b.lurl == null || b.lurl.isEmpty()) continue;
                String url = AuctionMacros.expandLoss(
                        b.lurl, requestId, b.impid, b.id,
                        winner.price, response.cur != null ? response.cur : "USD",
                        AuctionMacros.LOSS_LOST_TO_HIGHER_BID);
                try {
                    networkClient.fireTrackingUrl(url);
                } catch (Exception e) {
                    AdLog.d("OpenRTBAdRepository: loss notice failed — %s", e.getMessage());
                }
            }
        }
    }
}
