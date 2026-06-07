package com.apexads.sdk.nativeads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.ApexAds;
import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.NativeAdPayload;
import com.apexads.sdk.core.mvvm.AdRepository;
import com.apexads.sdk.core.mvvm.AdState;
import com.apexads.sdk.core.mvvm.AdStateObserver;
import com.apexads.sdk.core.mvvm.AdViewModelListener;
import com.apexads.sdk.core.mvvm.OpenRTBAdRepository;
import com.apexads.sdk.core.request.OpenRTBRequestBuilder;
import com.apexads.sdk.core.utils.AdLog;

/**
 * Native ad facade (IAB OpenRTB Native 1.2).
 *
 * <p>Thin wrapper over {@link NativeAdViewModel}. Native JSON parsing is
 * performed inside the ViewModel's {@link NativeAdViewModel#onAdLoaded} hook —
 * this facade never touches the raw markup.
 *
 * <pre>{@code
 * NativeAd ad = new NativeAd.Builder("placement-003")
 *     .listener(myListener)
 *     .build();
 * ad.load();
 * // in onNativeAdLoaded():
 * ad.bindTo(nativeAdView);
 * }</pre>
 */
public final class NativeAd {

    private final NativeAdViewModel viewModel;
    @Nullable private final NativeAdListener listener;

    private NativeAd(Builder builder) {
        AdRepository repository = new OpenRTBAdRepository(
                ApexAds.getNetworkClient(),
                new OpenRTBRequestBuilder(
                        ApexAds.getDeviceInfoProvider(),
                        ApexAds.getConsentManager()));

        viewModel = new NativeAdViewModel(
                repository,
                new AdCache(),
                builder.placementId != null ? builder.placementId : "",
                new NativeAdParser());

        this.listener = builder.listener;

        // Bridge generic AdViewModelListener → NativeAdListener
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

    // ── Publisher API ─────────────────────────────────────────────────────────

    /** Fetches and parses the native ad. Calls listener on the main thread. */
    public void load() {
        viewModel.load();
    }

    /**
     * Binds loaded ad assets to {@code view}.
     * Must be called after {@link NativeAdListener#onNativeAdLoaded}.
     */
    public void bindTo(@NonNull NativeAdView view) {
        NativeAdPayload payload = viewModel.getNativePayload();
        if (payload == null) {
            AdLog.w("NativeAd: bindTo() called before ad was loaded");
            return;
        }
        view.bind(payload, ApexAds.getNetworkClient());
    }

    /** {@code true} when native assets are parsed and ready to bind. */
    public boolean isReady() {
        return viewModel.isReady();
    }

    /** Returns the current {@link AdState}. */
    @NonNull
    public AdState getState() {
        return viewModel.getState();
    }

    /** Subscribes a raw {@link AdStateObserver}; receives immediate state delivery. */
    public void addStateObserver(@NonNull AdStateObserver observer) {
        viewModel.getStateObservable().addObserver(observer);
    }

    /** Removes a previously added {@link AdStateObserver}. */
    public void removeStateObserver(@NonNull AdStateObserver observer) {
        viewModel.getStateObservable().removeObserver(observer);
    }

    /** Releases the ViewModel and cache entry. */
    public void destroy() {
        viewModel.destroy();
    }

    // ── Convenience payload accessors (unchanged public API) ──────────────────

    @Nullable public String getTitle() { NativeAdPayload p = viewModel.getNativePayload(); return p != null ? p.title : null; }
    @Nullable public String getDescription() { NativeAdPayload p = viewModel.getNativePayload(); return p != null ? p.description : null; }
    @Nullable public String getCtaText() { NativeAdPayload p = viewModel.getNativePayload(); return p != null ? p.ctaText : null; }
    @Nullable public String getIconUrl() { NativeAdPayload p = viewModel.getNativePayload(); return p != null ? p.iconUrl : null; }
    @Nullable public String getImageUrl() { NativeAdPayload p = viewModel.getNativePayload(); return p != null ? p.imageUrl : null; }
    @Nullable public String getAdvertiserName() { NativeAdPayload p = viewModel.getNativePayload(); return p != null ? p.advertiserName : null; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static final class Builder {
        private final String placementId;
        @Nullable private NativeAdListener listener;

        public Builder(@Nullable String placementId) { this.placementId = placementId; }

        public Builder listener(@NonNull NativeAdListener l) { listener = l; return this; }

        @NonNull
        public NativeAd build() {
            if (!ApexAds.isInitialized()) {
                throw new IllegalStateException(
                        "Call ApexAds.init() before creating ad instances.");
            }
            return new NativeAd(this);
        }
    }
}
