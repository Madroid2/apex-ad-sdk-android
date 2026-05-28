package com.apexads.sdk.core.network;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;
import com.apexads.sdk.core.utils.AdLog;

/**
 * {@link AdNetworkClient} that tries the live Apex Ad Server first and falls
 * back to {@link MockAdExchange} on any of the following conditions:
 *
 * <ul>
 *   <li>Network error or timeout reaching the ad server</li>
 *   <li>Server returns HTTP ≥ 300</li>
 *   <li>Response JSON is unparseable</li>
 *   <li>Response contains no seat-bids (server-side no-fill)</li>
 *   <li>Winning bid has an empty or missing {@code adm}</li>
 *   <li>Winning bid has price ≤ 0</li>
 * </ul>
 *
 * This keeps the demo app fully functional before real DSPs are enrolled:
 * the live server's house campaigns and MockDSP will fill most requests,
 * and the in-SDK {@link MockAdExchange} catches anything that slips through.
 */
public final class FallbackAdNetworkClient implements AdNetworkClient {

    private final HttpAdNetworkClient primary;
    private final MockAdExchange fallback;

    public FallbackAdNetworkClient(@NonNull HttpAdNetworkClient primary) {
        this.primary = primary;
        this.fallback = new MockAdExchange();
    }

    @NonNull
    @Override
    public BidResponse requestBid(@NonNull BidRequest request) throws Exception {
        BidResponse response = null;

        try {
            response = primary.requestBid(request);
        } catch (Exception e) {
            AdLog.w("ApexNet primary failed (%s) — using mock fallback", e.getMessage());
        }

        if (isNoFill(response)) {
            AdLog.d("ApexNet primary returned no-fill — using mock fallback");
            return fallback.requestBid(request);
        }

        return response;
    }

    @Override
    public void fireTrackingUrl(@NonNull String url) {
        // Delegate to primary — real tracking only fires for live server responses.
        // MockAdExchange tracking URLs are no-ops by design.
        primary.fireTrackingUrl(url);
    }

    /**
     * Returns true when the response should be treated as a no-fill and the
     * fallback mock should be used instead.
     */
    private static boolean isNoFill(BidResponse response) {
        if (response == null) return true;
        if (response.seatbid == null || response.seatbid.isEmpty()) return true;

        BidResponse.Bid winner = response.getWinningBid();
        if (winner == null) return true;
        if (winner.adm == null || winner.adm.trim().isEmpty()) return true;
        if (winner.price <= 0) return true;

        return false;
    }
}
