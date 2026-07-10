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
import com.apexads.sdk.interstitial.presentation.view.InterstitialActivity;

public final class InterstitialAdViewModel extends AdViewModel {

    private static final String TAG = "InterstitialAdViewModel";

    InterstitialAdViewModel(
            @NonNull AdRepository repository,
            @NonNull AdCache cache,
            @NonNull String placementId) {
        super(repository, cache, AdFormat.INTERSTITIAL, AdSize.INTERSTITIAL_FULL, placementId, 0.0);
    }

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
