package com.apexads.sdk.nativeads;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.NativeAdPayload;
import com.apexads.sdk.core.domain.repository.AdRepository;
import com.apexads.sdk.core.presentation.mvvm.AdState;
import com.apexads.sdk.core.presentation.mvvm.AdStateObserver;
import com.apexads.sdk.core.presentation.mvvm.AdViewModelListener;
import com.apexads.sdk.core.data.repository.OpenRTBAdRepository;
import com.apexads.sdk.core.request.OpenRTBRequestBuilder;
import com.apexads.sdk.core.utils.AdLog;
import com.apexads.sdk.core.utils.AdUrlHandler;
import com.apexads.sdk.internal.ApexSdkRuntime;

public final class NativeAd {

    private final NativeAdViewModel viewModel;
    @Nullable private final NativeAdListener listener;

    private NativeAd(Builder builder) {
        AdRepository repository = new OpenRTBAdRepository(
                ApexSdkRuntime.getNetworkClient(),
                new OpenRTBRequestBuilder(
                        ApexSdkRuntime.getDeviceInfoProvider(),
                        ApexSdkRuntime.getConsentManager()));

        viewModel = new NativeAdViewModel(
                repository,
                new AdCache(),
                builder.placementId != null ? builder.placementId : "",
                new NativeAdParser());

        this.listener = builder.listener;

        viewModel.setViewListener(new AdViewModelListener() {
            @Override
            public void onAdLoaded(@NonNull AdData adData) {
                if (listener != null) listener.onNativeAdLoaded(NativeAd.this);
            }

            @Override
            public void onAdFailed(@NonNull AdError error) {
                if (listener != null) listener.onNativeAdFailed(error);
            }

            @Override
            public void onAdExpired() {
                if (listener != null)
                    listener.onNativeAdFailed(new AdError.NoFill("Cached ad expired"));
            }
        });
    }

    public void load() {
        viewModel.load();
    }

    public void bindTo(@NonNull NativeAdView view) {
        NativeAdPayload payload = viewModel.getNativePayload();
        if (payload == null) {
            AdLog.w("NativeAd: bindTo() called before ad was loaded");
            return;
        }
        view.bind(payload, ApexSdkRuntime.getTrackingClient());
    }

    /**
     * Triggers the ad's click-through for a custom-rendered native layout
     * (e.g. Jetpack Compose), where the publisher draws the assets themselves
     * instead of binding a {@link NativeAdView}. Opens the click URL in the
     * browser and notifies {@code onNativeAdClicked}. Wire this to the
     * onClick of your CTA (and/or the whole ad card).
     *
     * @return true if a click-through was opened.
     */
    public boolean handleClick(@NonNull Context context) {
        NativeAdPayload payload = viewModel.getNativePayload();
        if (payload == null || payload.clickUrl == null) {
            AdLog.w("NativeAd: handleClick() before load, or ad has no click URL");
            return false;
        }
        boolean opened = AdUrlHandler.openExternalUrl(context, payload.clickUrl, "NativeAd");
        if (opened && listener != null) {
            listener.onNativeAdClicked();
        }
        return opened;
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

    @Nullable public String getTitle() { NativeAdPayload p = viewModel.getNativePayload(); return p != null ? p.title : null; }
    @Nullable public String getDescription() { NativeAdPayload p = viewModel.getNativePayload(); return p != null ? p.description : null; }
    @Nullable public String getCtaText() { NativeAdPayload p = viewModel.getNativePayload(); return p != null ? p.ctaText : null; }
    @Nullable public String getIconUrl() { NativeAdPayload p = viewModel.getNativePayload(); return p != null ? p.iconUrl : null; }
    @Nullable public String getImageUrl() { NativeAdPayload p = viewModel.getNativePayload(); return p != null ? p.imageUrl : null; }
    @Nullable public String getAdvertiserName() { NativeAdPayload p = viewModel.getNativePayload(); return p != null ? p.advertiserName : null; }

    public static final class Builder {
        private final String placementId;
        @Nullable private NativeAdListener listener;

        public Builder(@Nullable String placementId) { this.placementId = placementId; }

        public Builder listener(@NonNull NativeAdListener l) { listener = l; return this; }

        @NonNull
        public NativeAd build() {
            if (!ApexSdkRuntime.isInitialized()) {
                throw new IllegalStateException(
                        "Call ApexAds.init() before creating ad instances.");
            }
            return new NativeAd(this);
        }
    }
}
