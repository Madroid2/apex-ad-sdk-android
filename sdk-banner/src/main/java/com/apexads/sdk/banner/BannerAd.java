package com.apexads.sdk.banner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.domain.repository.AdRepository;
import com.apexads.sdk.core.presentation.mvvm.AdState;
import com.apexads.sdk.core.presentation.mvvm.AdStateObserver;
import com.apexads.sdk.core.presentation.mvvm.AdViewModelListener;
import com.apexads.sdk.core.data.repository.OpenRTBAdRepository;
import com.apexads.sdk.core.request.OpenRTBRequestBuilder;
import com.apexads.sdk.core.utils.AdLog;
import com.apexads.sdk.internal.ApexSdkRuntime;

public final class BannerAd {

    private final BannerAdViewModel viewModel;
    @Nullable private final BannerAdListener listener;

    private BannerAd(Builder builder) {
        AdRepository repository = new OpenRTBAdRepository(
                ApexSdkRuntime.getNetworkClient(),
                new OpenRTBRequestBuilder(
                        ApexSdkRuntime.getDeviceInfoProvider(),
                        ApexSdkRuntime.getConsentManager()));

        viewModel = new BannerAdViewModel(
                repository,
                new AdCache(),
                builder.placementId != null ? builder.placementId : "",
                builder.adSize,
                builder.bidFloor);

        this.listener = builder.listener;

        viewModel.setViewListener(new AdViewModelListener() {
            @Override
            public void onAdLoaded(@NonNull AdData adData) {
                if (listener != null) listener.onAdLoaded();
            }

            @Override
            public void onAdFailed(@NonNull AdError error) {
                if (listener != null) listener.onAdFailed(error);
            }

            @Override
            public void onAdExpired() {
                AdLog.d("BannerAd: ad expired — call load() to refresh");
                if (listener != null)
                    listener.onAdFailed(new com.apexads.sdk.core.error.AdError.NoFill("Cached ad expired"));
            }
        });
    }

    public void load() {
        viewModel.load();
    }

    public void show(@NonNull BannerAdView view) {
        if (viewModel.checkAndMarkExpired()) {
            return;
        }
        AdData data = viewModel.getAdData();
        if (data == null) {
            AdLog.w("BannerAd: show() called before ad was loaded");
            return;
        }

        view.bind(viewModel, listener);

        view.render(data);
        viewModel.onDisplayed();
    }

    @NonNull
    public AdState getState() {
        return viewModel.getState();
    }

    public void addStateObserver(@NonNull AdStateObserver observer) {
        viewModel.getStateObservable().addObserver(observer);
    }

    public void removeStateObserver(@NonNull AdStateObserver observer) {
        viewModel.getStateObservable().removeObserver(observer);
    }

    public void destroy() {
        viewModel.destroy();
    }

    public static final class Builder {
        private final String placementId;
        private AdSize adSize = AdSize.BANNER_320x50;
        private double bidFloor = 0.0;
        @Nullable private BannerAdListener listener;

        public Builder(@Nullable String placementId) {
            this.placementId = placementId;
        }

        public Builder adSize(@NonNull AdSize size) { adSize = size; return this; }
        public Builder bidFloor(double floor) { bidFloor = floor; return this; }
        public Builder listener(@NonNull BannerAdListener l) { listener = l; return this; }

        @NonNull
        public BannerAd build() {
            if (!ApexSdkRuntime.isInitialized()) {
                throw new IllegalStateException(
                        "Call ApexAds.init() before creating ad instances.");
            }
            return new BannerAd(this);
        }
    }
}
