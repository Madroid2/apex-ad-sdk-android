package com.apexads.sdk.core.network;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;

/**
 * Network contract for ad exchange communication.
 *
 * Both methods are blocking and must be called off the main thread.
 * Ad loaders use {@link SdkExecutors#IO} to dispatch these calls.
 */
public interface AdNetworkClient {

    /** Fires an OpenRTB auction. Blocking — call from a worker thread. */
    @NonNull
    BidResponse requestBid(@NonNull BidRequest request) throws Exception;

    /**
     * Fires a tracking pixel (impression, click, win-notice, billing).
     * Must not throw — tracking failure must never surface to the publisher.
     */
    void fireTrackingUrl(@NonNull String url);
}
