package com.apexads.sdk.core.network;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;

public interface AdNetworkClient {

    @NonNull
    BidResponse requestBid(@NonNull BidRequest request) throws Exception;

    void fireTrackingUrl(@NonNull String url);
}
