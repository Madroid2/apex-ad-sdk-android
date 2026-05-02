package com.apexads.sdk.interstitial;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.error.AdError;

public interface InterstitialAdListener {
    void onInterstitialLoaded();
    void onInterstitialFailed(@NonNull AdError error);
    default void onInterstitialShown() {}
    default void onInterstitialClosed() {}
    default void onInterstitialClicked() {}
    /** Called when the user successfully saves the wallet pass from this interstitial. */
    default void onWalletPassSaved() {}
    /** Called when the user cancels the wallet save flow. */
    default void onWalletPassCancelled() {}
    /** Called when the wallet save flow fails (e.g. Google Wallet not available). */
    default void onWalletPassFailed() {}
}
