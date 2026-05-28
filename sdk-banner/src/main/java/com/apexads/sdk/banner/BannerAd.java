package com.apexads.sdk.banner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.ApexAds;
import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.mvvm.AdRepository;
import com.apexads.sdk.core.mvvm.AdState;
import com.apexads.sdk.core.mvvm.AdStateObserver;
import com.apexads.sdk.core.mvvm.AdViewModelListener;
import com.apexads.sdk.core.mvvm.OpenRTBAdRepository;
import com.apexads.sdk.core.request.OpenRTBRequestBuilder;
import com.apexads.sdk.core.utils.AdLog;

/**
 * Publisher-facing banner ad facade.
 *
 * <p>This class is a <em>thin facade</em> over {@link BannerAdViewModel}. All
 * business logic — auction dispatch, caching, state management, TTL enforcement —
 * lives in the ViewModel. The facade's only responsibilities are:
 * <ul>
 *   <li>Providing the familiar {@code load()} / {@code show(view)} / {@code destroy()}
 *       publisher API (backward-compatible).</li>
 *   <li>Bridging the generic {@link AdViewModelListener} contract to the
 *       banner-specific {@link BannerAdListener}.</li>
 *   <li>Binding the {@link BannerAdView} to the ViewModel after a successful load.</li>
 * </ul>
 *
 * <pre>{@code
 * BannerAd banner = new BannerAd.Builder("placement-001")
 *     .adSize(AdSize.BANNER_320x50)
 *     .listener(myListener)
 *     .build();
 * banner.load();
 * // in onAdLoaded():
 * banner.show(bannerAdView);
 * }</pre>
 */
public final class BannerAd {

    private final BannerAdViewModel viewModel;
    @Nullable private final BannerAdListener listener;

    private BannerAd(Builder builder) {
        AdRepository repository = new OpenRTBAdRepository(
                ApexAds.getNetworkClient(),
                new OpenRTBRequestBuilder(
                        ApexAds.getDeviceInfoProvider(),
                        ApexAds.getConsentManager()));

        viewModel = new BannerAdViewModel(
                repository,
                new AdCache(),
                builder.placementId != null ? builder.placementId : "",
                builder.adSize,
                builder.bidFloor);

        this.listener = builder.listener;

        // Bridge AdViewModelListener → BannerAdListener
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

    // ── Publisher API ─────────────────────────────────────────────────────────

    /**
     * Fetches an ad from the exchange (or serves from cache). Non-blocking.
     * Calls {@link BannerAdListener#onAdLoaded()} or {@link BannerAdListener#onAdFailed}
     * on the main thread when complete.
     */
    public void load() {
        viewModel.load();
    }

    /**
     * Renders the loaded ad into {@code view}.
     *
     * <p>Must be called after {@link BannerAdListener#onAdLoaded()}.
     * The view subscribes to the ViewModel's {@link com.apexads.sdk.core.mvvm.AdStateObservable}
     * so it will automatically reflect future state changes (e.g. EXPIRED).
     */
    public void show(@NonNull BannerAdView view) {
        if (viewModel.checkAndMarkExpired()) {
            return;
        }
        AdData data = viewModel.getAdData();
        if (data == null) {
            AdLog.w("BannerAd: show() called before ad was loaded");
            return;
        }
        // Bind the view to the ViewModel for reactive state updates
        view.bind(viewModel, listener);
        // Render the creative immediately
        view.render(data);
        viewModel.onDisplayed();
    }

    /**
     * Returns the current {@link AdState} — useful for conditional show logic
     * without keeping a separate boolean flag.
     */
    @NonNull
    public AdState getState() {
        return viewModel.getState();
    }

    /**
     * Subscribes a raw {@link AdStateObserver} to the underlying observable.
     * Receives immediate delivery of the current state on subscribe.
     */
    public void addStateObserver(@NonNull AdStateObserver observer) {
        viewModel.getStateObservable().addObserver(observer);
    }

    /** Removes a previously added {@link AdStateObserver}. */
    public void removeStateObserver(@NonNull AdStateObserver observer) {
        viewModel.getStateObservable().removeObserver(observer);
    }

    /**
     * Releases the ViewModel and cache entry. Call when the host component
     * (Activity / Fragment) is permanently destroyed.
     */
    public void destroy() {
        viewModel.destroy();
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static final class Builder {
        private final String placementId;
        private AdSize adSize = AdSize.BANNER_320x50;
        private double bidFloor = 0.0;
        @Nullable private BannerAdListener listener;

        public Builder(@Nullable String placementId) {
            this.placementId = placementId;
        }

        public Builder adSize(@NonNull AdSize size)         { adSize = size;       return this; }
        public Builder bidFloor(double floor)               { bidFloor = floor;    return this; }
        public Builder listener(@NonNull BannerAdListener l){ listener = l;        return this; }

        @NonNull
        public BannerAd build() {
            if (!ApexAds.isInitialized()) {
                throw new IllegalStateException(
                        "Call ApexAds.init() before creating ad instances.");
            }
            return new BannerAd(this);
        }
    }
}
