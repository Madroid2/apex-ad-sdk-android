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

/**
 * Rewarded / pre-roll video ad facade backed by VAST 4.0.
 *
 * <p>Thin wrapper over {@link VideoAdViewModel}. VAST XML parsing is performed
 * inside the ViewModel's {@link VideoAdViewModel#onAdLoaded} hook — this facade
 * never touches the raw markup.
 *
 * <pre>{@code
 * VideoAd ad = new VideoAd.Builder("placement-004")
 *     .listener(myListener)
 *     .build();
 * ad.load();
 * // in onVideoAdLoaded():
 * ad.show(activity);
 * }</pre>
 */
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

        // Bridge generic AdViewModelListener → VideoAdListener
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

    // ── Publisher API ─────────────────────────────────────────────────────────

    /** Fetches and parses the VAST creative. Calls listener on the main thread. */
    public void load() {
        viewModel.load();
    }

    /**
     * Launches {@link VideoAdActivity}.
     * Must be called after {@link VideoAdListener#onVideoAdLoaded()}.
     */
    public void show(@NonNull Context context) {
        viewModel.show(context, listener != null ? listener : NO_OP_LISTENER);
    }

    /** {@code true} when VAST is parsed and the creative is ready to play. */
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

    // ── Builder ───────────────────────────────────────────────────────────────

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

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static final VideoAdListener NO_OP_LISTENER = new VideoAdListener() {
        @Override public void onVideoAdLoaded() {}
        @Override public void onVideoAdFailed(@NonNull AdError error) {}
    };
}
