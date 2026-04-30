package com.apexads.sdk.inappbidding;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Price signal passed to a mediation platform (MAX, LevelPlay, etc.) as a
 * server-to-server bid token. Carries the winning CPM so the waterfall can
 * compare it against other demand sources.
 */
public final class BidToken {

    /** Bid price in CPM USD. */
    public final double cpmUsd;
    /** Opaque token forwarded to the ad server for win-price verification. */
    public final String token;
    /** Placement this bid was fetched for. */
    public final String placementId;
    /** Epoch millis when this token was created — tokens expire after {@link #TTL_MS}. */
    public final long createdAtMs;

    public static final long TTL_MS = 30_000L; // 30 s

    BidToken(@NonNull String placementId, double cpmUsd, @NonNull String token) {
        this.placementId = placementId;
        this.cpmUsd = cpmUsd;
        this.token = token;
        this.createdAtMs = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAtMs > TTL_MS;
    }

    @NonNull
    @Override
    public String toString() {
        return "BidToken{placementId='" + placementId + "', cpm=" + cpmUsd + ", expired=" + isExpired() + "}";
    }
}
