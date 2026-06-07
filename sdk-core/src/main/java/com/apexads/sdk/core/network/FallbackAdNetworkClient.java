package com.apexads.sdk.core.network;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;

/**
 * @deprecated Superseded by {@link WaterfallAdNetworkClient}.
 *
 * <p>This client previously fell back to an in-process {@link MockAdExchange}
 * whenever the live Apex Ad Server errored or returned a no-fill. That fabricates
 * demand and must never run in a real publisher integration, so the mock fallback
 * has been <b>removed</b>: this client now simply delegates to the live primary and
 * returns its honest response (no-fill included).
 *
 * <p>For development/CI mock fill, set {@code ApexAdsConfig.Builder.debugFakeFill(true)};
 * {@link com.apexads.sdk.ApexAds#init} then appends a {@link MockAdExchange} as the
 * lowest-priority source of a {@link WaterfallAdNetworkClient}. Prefer that path.
 */
@Deprecated
public final class FallbackAdNetworkClient implements AdNetworkClient {

    private final HttpAdNetworkClient primary;

    public FallbackAdNetworkClient(@NonNull HttpAdNetworkClient primary) {
        this.primary = primary;
    }

    @NonNull
    @Override
    public BidResponse requestBid(@NonNull BidRequest request) throws Exception {
        // Honest by default: return the live server's response, even when it is a
        // no-fill. No mock demand is ever substituted here.
        return primary.requestBid(request);
    }

    @Override
    public void fireTrackingUrl(@NonNull String url) {
        primary.fireTrackingUrl(url);
    }
}
