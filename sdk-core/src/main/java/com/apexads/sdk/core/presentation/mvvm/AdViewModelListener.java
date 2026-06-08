package com.apexads.sdk.core.presentation.mvvm;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;

public interface AdViewModelListener {

    void onAdLoaded(@NonNull AdData adData);

    void onAdFailed(@NonNull AdError error);

    default void onAdDisplayed() {}

    default void onAdExpired() {}
}
