package com.apexads.sdk.video;

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
import com.apexads.sdk.video.vast.VastParser;

public final class VideoAd {

    private final VideoAdViewModel viewModel;
    @Nullable private final VideoAdListener listener;

    private VideoAd(Builder builder) {
        AdRepository repository = new OpenRTBAdRepository(
                ApexAds.getNetworkClient(),
                new OpenRTBRequestBuilder(
                        ApexAds.getDeviceInfoProvider(),
                        ApexAds.getConsentManager()));

        viewModel = new VideoAdViewModel(
                repository,
                new AdCache(),
                builder.placementId != null ? builder.placementId : "",
                new VastParser(),
                ApexAds.getNetworkClient());

        this.listener = builder.listener;

        viewModel.setViewListener(new AdViewModelListener() {
            @Override
            public void onAdLoaded(@NonNull AdData adData) {
                if (listener != null) listener.onVideoAdLoaded();
            }

            @Override
            public void onAdFailed(@NonNull AdError error) {
                if (listener != null) listener.onVideoAdFailed(error);
            }

            @Override
            public void onAdExpired() {
                if (listener != null)
                    listener.onVideoAdFailed(new AdError.NoFill("Cached ad expired"));
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
        @Nullable private VideoAdListener listener;

        public Builder(@Nullable String placementId) { this.placementId = placementId; }

        public Builder listener(@NonNull VideoAdListener l) { listener = l; return this; }

        @NonNull
        public VideoAd build() {
            if (!ApexAds.isInitialized()) {
                throw new IllegalStateException(
                        "Call ApexAds.init() before creating ad instances.");
            }
            return new VideoAd(this);
        }
    }

    private static final VideoAdListener NO_OP_LISTENER = new VideoAdListener() {
        @Override public void onVideoAdLoaded() {}
        @Override public void onVideoAdFailed(@NonNull AdError error) {}
    };
}
