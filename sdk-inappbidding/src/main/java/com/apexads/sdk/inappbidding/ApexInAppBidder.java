package com.apexads.sdk.inappbidding;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.ApexAds;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;
import com.apexads.sdk.core.network.AdNetworkClient;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.request.OpenRTBRequestBuilder;
import com.apexads.sdk.core.utils.AdLog;

import java.util.UUID;

/**
 * Fetches a real-time bid from the ApexAds ad server and packages it as a
 * {@link BidToken} price signal for header-bidding integration with MAX or LevelPlay.
 *
 * Typical flow:
 * <pre>{@code
 * ApexInAppBidder.fetchBidToken("placement-001", AdFormat.BANNER, new InAppBidListener() {
 *     public void onBidReady(BidToken token) {
 *         maxInterstitialAd.setLocalExtraParameter("apex_bid_token", token.token);
 *         maxInterstitialAd.setLocalExtraParameter("apex_bid_cpm", String.valueOf(token.cpmUsd));
 *         maxInterstitialAd.loadAd();
 *     }
 *     public void onBidFailed(AdError error) { maxInterstitialAd.loadAd(); }
 * });
 * }</pre>
 */
public final class ApexInAppBidder {

    private ApexInAppBidder() {}

    public static void fetchBidToken(@NonNull String placementId,
                                     @NonNull AdFormat format,
                                     @NonNull InAppBidListener listener) {
        if (!ApexAds.isInitialized()) {
            SdkExecutors.MAIN.post(() ->
                    listener.onBidFailed(new AdError.Network("ApexAds SDK not initialized", null)));
            return;
        }

        AdNetworkClient client = ApexAds.getNetworkClient();
        OpenRTBRequestBuilder builder = new OpenRTBRequestBuilder(
                ApexAds.getDeviceInfoProvider(), ApexAds.getConsentManager());

        SdkExecutors.IO.execute(() -> {
            try {
                BidRequest request = builder.adFormat(format).placementId(placementId).build();
                BidResponse response = client.requestBid(request);
                BidResponse.Bid bid = response.getWinningBid();

                if (bid == null || bid.price <= 0) {
                    SdkExecutors.MAIN.post(() -> listener.onBidFailed(new AdError.NoFill()));
                    return;
                }

                String token = buildToken(bid, request.id);
                BidToken bidToken = new BidToken(placementId, bid.price, token);
                AdLog.i("ApexInAppBidder: bid ready cpm=%.3f placement=%s", bid.price, placementId);
                SdkExecutors.MAIN.post(() -> listener.onBidReady(bidToken));

            } catch (Exception e) {
                AdLog.e(e, "ApexInAppBidder: bid fetch failed");
                SdkExecutors.MAIN.post(() -> listener.onBidFailed(new AdError.Network(e.getMessage(), e)));
            }
        });
    }

    private static String buildToken(@NonNull BidResponse.Bid bid, @Nullable String requestId) {
        // Opaque token: base64-like encoding of price + request context (server verifies on win)
        String raw = (requestId != null ? requestId : UUID.randomUUID().toString())
                + "|" + bid.price
                + "|" + (bid.adid != null ? bid.adid : "")
                + "|" + System.currentTimeMillis();
        return android.util.Base64.encodeToString(raw.getBytes(), android.util.Base64.NO_WRAP);
    }
}
