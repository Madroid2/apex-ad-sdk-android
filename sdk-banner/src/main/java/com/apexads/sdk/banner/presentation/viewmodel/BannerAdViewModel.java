package com.apexads.sdk.banner;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.mvvm.AdRepository;
import com.apexads.sdk.core.mvvm.AdViewModel;

/**
 * ViewModel for {@link BannerAd}.
 *
 * <p>Banner creatives are MRAID HTML — no format-specific post-processing is
 * required. The base-class {@link #load()} / {@link #onDisplayed()} / {@link #destroy()}
 * lifecycle covers everything; this subclass exists to anchor the Banner format
 * identity and to provide a typed hook for future banner-specific logic
 * (e.g. viewability measurement, MRAID state tracking).
 */
public final class BannerAdViewModel extends AdViewModel {

    BannerAdViewModel(
            @NonNull AdRepository repository,
            @NonNull AdCache cache,
            @NonNull String placementId,
            @NonNull AdSize adSize,
            double bidFloor) {
        super(repository, cache, AdFormat.BANNER, adSize, placementId, bidFloor);
    }

    /**
     * Pass-through — banner ad markup (MRAID HTML) requires no pre-render
     * transformation at the ViewModel layer.
     */
    @NonNull
    @Override
    protected AdData onAdLoaded(@NonNull AdData adData) {
        return adData;
    }
}
