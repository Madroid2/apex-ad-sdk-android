package com.apexads.sdk.core.network;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;

@Deprecated
public final class FallbackAdNetworkClient implements AdNetworkClient {

    private final HttpAdNetworkClient primary;

    public FallbackAdNetworkClient(@NonNull HttpAdNetworkClient primary) {
        this.primary = primary;
    }

    @NonNull
    @Override
    public BidResponse requestBid(@NonNull BidRequest request) throws Exception {

        return primary.requestBid(request);
    }

    @Override
    public void fireTrackingUrl(@NonNull String url) {
        primary.fireTrackingUrl(url);
    }
}
