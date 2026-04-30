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
}
