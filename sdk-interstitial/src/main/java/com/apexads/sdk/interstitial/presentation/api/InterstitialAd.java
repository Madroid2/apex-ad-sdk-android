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

/**
 * Fullscreen interstitial ad facade.
 *
 * <p>Thin wrapper over {@link InterstitialAdViewModel}. Pre-load at a natural
 * pause point; call {@link #show(Context)} when ready.
 *
 * <pre>{@code
 * InterstitialAd ad = new InterstitialAd.Builder("placement-002")
 *     .listener(myListener)
 *     .build();
 * ad.load();
 * // ... at a content transition:
 * if (ad.isReady()) ad.show(activity);
 * }</pre>
 */
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

        // Bridge generic AdViewModelListener → InterstitialAdListener
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

    // ── Publisher API ─────────────────────────────────────────────────────────

    /** Fetches the ad. Calls listener on the main thread when done. */
    public void load() {
        viewModel.load();
    }

    /**
     * Launches the fullscreen ad.
     * Guard with {@link #isReady()} before calling.
     */
    public void show(@NonNull Context context) {
        viewModel.show(context, listener != null ? listener : NO_OP_LISTENER);
    }

    /** {@code true} when a non-expired creative is ready to display. */
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

    // ── Internal helpers ──────────────────────────────────────────────────────

    private static final InterstitialAdListener NO_OP_LISTENER = new InterstitialAdListener() {
        @Override public void onInterstitialLoaded() {}
        @Override public void onInterstitialFailed(@NonNull AdError error) {}
    };
}
