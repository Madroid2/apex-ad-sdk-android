package com.apexads.sdk.inappbidding;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.error.AdError;

/**
 * Callback for {@link ApexInAppBidder#fetchBidToken}.
 */
public interface InAppBidListener {

    /**
     * Called on the main thread when a bid is ready.
     * Pass the {@link BidToken} to your mediation platform's
     * {@code setLocalExtraParameter} / {@code setSignal} equivalent.
     */
    void onBidReady(@NonNull BidToken token);

    /** Called on the main thread when no bid is available or the request failed. */
    void onBidFailed(@NonNull AdError error);
}
