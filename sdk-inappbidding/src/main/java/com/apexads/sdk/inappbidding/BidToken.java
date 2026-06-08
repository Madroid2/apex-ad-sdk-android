package com.apexads.sdk.inappbidding;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class BidToken {

    public final double cpmUsd;

    public final String token;

    public final String placementId;

    public final long createdAtMs;

    public static final long TTL_MS = 30_000L;

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
