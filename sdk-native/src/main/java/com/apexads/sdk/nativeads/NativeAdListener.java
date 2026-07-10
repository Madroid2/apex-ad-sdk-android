package com.apexads.sdk.nativeads;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.error.AdError;

public interface NativeAdListener {
    void onNativeAdLoaded(@NonNull NativeAd ad);
    void onNativeAdFailed(@NonNull AdError error);
    default void onNativeAdClicked() {}
    default void onNativeAdImpression() {}
}
