package com.apexads.sdk.interstitial;

import android.content.Context;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.domain.repository.AdRepository;
import com.apexads.sdk.core.presentation.mvvm.AdViewModel;
import com.apexads.sdk.core.utils.AdLog;

/**
 * ViewModel for {@link InterstitialAd}.
 *
 * <p>Adds a {@link #show(Context, InterstitialAdListener)} method that launches
 * {@link InterstitialActivity} when a loaded creative is available and non-expired.
 * Delegates all auction, caching and state management to the base class.
 */
public final class InterstitialAdViewModel extends AdViewModel {

    private static final String TAG = "InterstitialAdViewModel";

    InterstitialAdViewModel(
            @NonNull AdRepository repository,
            @NonNull AdCache cache,
            @NonNull String placementId) {
        super(repository, cache, AdFormat.INTERSTITIAL, AdSize.INTERSTITIAL_FULL, placementId, 0.0);
    }

    /**
     * Launches the fullscreen interstitial. Safe to call only after the
     * {@link com.apexads.sdk.core.presentation.mvvm.AdViewModelListener#onAdLoaded} callback.
     *
     * @param context  Must be an Activity or Application context.
     * @param listener Publisher listener forwarded to {@link InterstitialActivity}.
     */
    public void show(@NonNull Context context, @NonNull InterstitialAdListener listener) {
        if (checkAndMarkExpired()) {
            AdLog.w(TAG + ": show() — ad expired, listener.onInterstitialFailed() fired");
            return;
        }

        AdData data = getAdData();
        if (data == null) {
            AdLog.w(TAG + ": show() called before ad was loaded — ignored");
            return;
        }

        InterstitialActivity.launch(context, data, listener);
        onDisplayed();
    }
}
