package com.apexads.sdk.banner;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.domain.repository.AdRepository;
import com.apexads.sdk.core.presentation.mvvm.AdViewModel;

public final class BannerAdViewModel extends AdViewModel {

    BannerAdViewModel(
            @NonNull AdRepository repository,
            @NonNull AdCache cache,
            @NonNull String placementId,
            @NonNull AdSize adSize,
            double bidFloor) {
        super(repository, cache, AdFormat.BANNER, adSize, placementId, bidFloor);
    }

    @NonNull
    @Override
    protected AdData onAdLoaded(@NonNull AdData adData) {
        return adData;
    }
}
