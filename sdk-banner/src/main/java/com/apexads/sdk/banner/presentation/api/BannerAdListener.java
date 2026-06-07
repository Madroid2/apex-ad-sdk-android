package com.apexads.sdk.banner;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.error.AdError;

public interface BannerAdListener {
    void onAdLoaded();
    void onAdFailed(@NonNull AdError error);
    default void onAdClicked() {}
    default void onAdExpanded() {}
    default void onAdClosed() {}
    default void onAdImpression() {}
    /** Called when the user successfully saves the wallet pass from this MRECT banner. */
    default void onWalletPassSaved() {}
    /** Called when the user cancels the wallet save flow. */
    default void onWalletPassCancelled() {}
    /** Called when the wallet save flow fails (e.g. Google Wallet not available). */
    default void onWalletPassFailed() {}
}
