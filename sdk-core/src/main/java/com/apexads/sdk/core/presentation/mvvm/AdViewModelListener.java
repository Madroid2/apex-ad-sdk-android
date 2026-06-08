package com.apexads.sdk.core.presentation.mvvm;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;

/**
 * Internal view contract between {@link AdViewModel} and the ad-unit facade
 * ({@code BannerAd}, {@code InterstitialAd}, etc.).
 *
 * <p>Modelled after Smaato ng-sdk-android's {@code SmaatoSdkViewModelListener}:
 * the ViewModel calls these methods to drive the view / notify the publisher
 * without holding a direct reference to the concrete ad class.
 *
 * <p>All callbacks are delivered on the <strong>main thread</strong>.
 */
public interface AdViewModelListener {

    /** Auction succeeded — {@code adData} holds the winning creative. */
    void onAdLoaded(@NonNull AdData adData);

    /** Auction or rendering failed. */
    void onAdFailed(@NonNull AdError error);

    /** The creative has been rendered into its container view. */
    default void onAdDisplayed() {}

    /** The creative TTL elapsed before it was shown — caller should reload. */
    default void onAdExpired() {}
}
