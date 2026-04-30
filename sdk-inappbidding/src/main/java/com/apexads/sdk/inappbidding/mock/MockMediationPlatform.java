package com.apexads.sdk.inappbidding.mock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.utils.AdLog;
import com.apexads.sdk.inappbidding.BidToken;

/**
 * Simulates a mediation platform (MAX / LevelPlay) for demo and POC purposes.
 *
 * In production you replace calls to this class with the real MAX or LevelPlay SDK.
 *
 * Simulated waterfall (CPM floors):
 *   Tier 1 — $5.00+  → ApexAds wins
 *   Tier 2 — $2.00+  → ApexAds wins
 *   Tier 3 — below   → Waterfall fallback (other network wins)
 */
public final class MockMediationPlatform {

    private static final double TIER1_FLOOR = 5.00;
    private static final double TIER2_FLOOR = 2.00;

    private @Nullable BidToken apexBidToken;

    public MockMediationPlatform() {}

    /** Equivalent to MAX's {@code setLocalExtraParameter} — stores the ApexAds bid signal. */
    public void setApexBidToken(@NonNull BidToken token) {
        this.apexBidToken = token;
        AdLog.d("MockMediationPlatform: bid token received cpm=%.3f", token.cpmUsd);
    }

    /**
     * Simulates waterfall load. Returns the winning network name.
     * In a real integration MAX/LevelPlay handles this comparison server-side.
     */
    public String simulateLoad() {
        if (apexBidToken == null || apexBidToken.isExpired()) {
            AdLog.d("MockMediationPlatform: no valid ApexAds token — waterfall fallback wins");
            return "WaterfallFallbackNetwork";
        }

        double cpm = apexBidToken.cpmUsd;
        String winner;
        if (cpm >= TIER1_FLOOR) {
            winner = "ApexAds (Tier1)";
        } else if (cpm >= TIER2_FLOOR) {
            winner = "ApexAds (Tier2)";
        } else {
            winner = "WaterfallFallbackNetwork";
        }

        AdLog.i("MockMediationPlatform: simulated auction winner=%s (apex_cpm=%.3f)", winner, cpm);
        return winner;
    }

    /** Simulates the impression callback from the mediation SDK. */
    public void simulateImpression(@NonNull OnImpressionListener listener) {
        String winner = simulateLoad();
        listener.onImpression(winner, apexBidToken != null ? apexBidToken.cpmUsd : 0.0);
    }

    public interface OnImpressionListener {
        void onImpression(@NonNull String winningNetwork, double cpm);
    }
}
