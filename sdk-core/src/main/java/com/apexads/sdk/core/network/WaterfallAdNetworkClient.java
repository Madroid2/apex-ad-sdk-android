package com.apexads.sdk.core.network;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;
import com.apexads.sdk.core.utils.AdLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WaterfallAdNetworkClient implements AdNetworkClient {

    private final List<AdNetworkClient> sources;

    public WaterfallAdNetworkClient(@NonNull List<AdNetworkClient> sources) {
        if (sources.isEmpty()) {
            throw new IllegalArgumentException(
                    "WaterfallAdNetworkClient requires at least one demand source");
        }
        this.sources = new ArrayList<>(sources);
    }

    @NonNull
    @Override
    public BidResponse requestBid(@NonNull BidRequest request) throws Exception {
        for (int i = 0; i < sources.size(); i++) {
            try {
                BidResponse response = sources.get(i).requestBid(request);
                if (!isNoFill(response)) {
                    AdLog.d("ApexWaterfall: source[%d] filled — serving real demand", i);
                    return response;
                }
                AdLog.d("ApexWaterfall: source[%d] no-fill — trying next", i);
            } catch (Exception e) {
                AdLog.w("ApexWaterfall: source[%d] error (%s) — trying next", i, e.getMessage());
            }
        }
        AdLog.d("ApexWaterfall: all %d source(s) returned no-fill", sources.size());
        return noFill(request.id);
    }

    @Override
    public void fireTrackingUrl(@NonNull String url) {

        sources.get(0).fireTrackingUrl(url);
    }

    public static boolean isNoFill(BidResponse response) {
        if (response == null) return true;
        if (response.seatbid == null || response.seatbid.isEmpty()) return true;

        BidResponse.Bid winner = response.getWinningBid();
        if (winner == null) return true;
        if (winner.adm == null || winner.adm.trim().isEmpty()) return true;
        return winner.price <= 0;
    }

    private static BidResponse noFill(String requestId) {
        BidResponse r = new BidResponse();
        r.id = requestId;
        r.seatbid = Collections.emptyList();
        r.nbr = 2;
        return r;
    }
}
