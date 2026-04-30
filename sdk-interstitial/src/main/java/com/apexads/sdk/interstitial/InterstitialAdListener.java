package com.apexads.sdk.interstitial;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.error.AdError;

public interface InterstitialAdListener {
    void onInterstitialLoaded();
    void onInterstitialFailed(@NonNull AdError error);
    default void onInterstitialShown() {}
    default void onInterstitialClosed() {}
    default void onInterstitialClicked() {}
}
