package com.apexads.sdk.interstitial;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.ApexAds;
import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.domain.repository.AdRepository;
import com.apexads.sdk.core.presentation.mvvm.AdState;
import com.apexads.sdk.core.presentation.mvvm.AdStateObserver;
import com.apexads.sdk.core.presentation.mvvm.AdViewModelListener;
import com.apexads.sdk.core.data.repository.OpenRTBAdRepository;
import com.apexads.sdk.core.request.OpenRTBRequestBuilder;

public final class InterstitialAd {

    private final InterstitialAdViewModel viewModel;
    @Nullable private final InterstitialAdListener listener;

    private InterstitialAd(Builder builder) {
        AdRepository repository = new OpenRTBAdRepository(
                ApexAds.getNetworkClient(),
                new OpenRTBRequestBuilder(
                        ApexAds.getDeviceInfoProvider(),
                        ApexAds.getConsentManager()));

        viewModel = new InterstitialAdViewModel(
                repository,
                new AdCache(),
                builder.placementId != null ? builder.placementId : "");

        this.listener = builder.listener;

        viewModel.setViewListener(new AdViewModelListener() {
            @Override
            public void onAdLoaded(@NonNull AdData adData) {
                if (listener != null) listener.onInterstitialLoaded();
            }

            @Override
            public void onAdFailed(@NonNull AdError error) {
                if (listener != null) listener.onInterstitialFailed(error);
            }

            @Override
            public void onAdExpired() {
                if (listener != null)
                    listener.onInterstitialFailed(new AdError.NoFill("Cached ad expired"));
            }
        });
    }

    public void load() {
        viewModel.load();
    }

    public void show(@NonNull Context context) {
        viewModel.show(context, listener != null ? listener : NO_OP_LISTENER);
    }

    public boolean isReady() {
        return viewModel.isReady();
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
        @Nullable private InterstitialAdListener listener;

        public Builder(@Nullable String placementId) {
            this.placementId = placementId;
        }

        public Builder listener(@NonNull InterstitialAdListener l) { listener = l; return this; }

        @NonNull
        public InterstitialAd build() {
            if (!ApexAds.isInitialized()) {
                throw new IllegalStateException(
                        "Call ApexAds.init() before creating ad instances.");
            }
            return new InterstitialAd(this);
        }
    }

    private static final InterstitialAdListener NO_OP_LISTENER = new InterstitialAdListener() {
        @Override public void onInterstitialLoaded() {}
        @Override public void onInterstitialFailed(@NonNull AdError error) {}
    };
}
