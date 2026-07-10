package com.apexads.sdk.video;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.error.AdError;

public interface VideoAdListener {
    void onVideoAdLoaded();
    void onVideoAdFailed(@NonNull AdError error);
    default void onVideoAdStarted() {}
    default void onVideoAdCompleted() {}
    default void onVideoAdSkipped() {}
    default void onVideoAdClicked() {}
    default void onRewardEarned() {}
}
