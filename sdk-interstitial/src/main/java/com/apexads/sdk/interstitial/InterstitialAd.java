package com.apexads.sdk.interstitial;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.ApexAds;
import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;
import com.apexads.sdk.core.network.AdNetworkClient;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.request.OpenRTBRequestBuilder;

import com.apexads.sdk.core.utils.AdLog;

/**
 * Fullscreen interstitial ad.
 *
 * Pre-load at a natural pause point; call {@link #show(Context)} when ready.
 *
 * <pre>{@code
 * InterstitialAd ad = new InterstitialAd.Builder("placement-002")
 *     .listener(myListener)
 *     .build();
 * ad.load();
 * // ... later, at a content transition:
 * if (ad.isReady()) ad.show(activity);
 * }</pre>
 */
public final class InterstitialAd {

    private final String placementId;
    private final InterstitialAdListener listener;
    private final AdNetworkClient networkClient;
    private final AdCache cache;
    private final OpenRTBRequestBuilder requestBuilder;

    private volatile AdData adData;

    private InterstitialAd(Builder builder) {
        this.placementId    = builder.placementId;
        this.listener       = builder.listener;
        this.networkClient  = ApexAds.getNetworkClient();
        this.cache          = new AdCache();
        this.requestBuilder = new OpenRTBRequestBuilder(
                ApexAds.getDeviceInfoProvider(), ApexAds.getConsentManager());
    }

    public void load() {
        AdData cached = cache.get(AdFormat.INTERSTITIAL, placementId);
        if (cached != null) {
            adData = cached;
            postToMain(() -> { if (listener != null) listener.onInterstitialLoaded(); });
            return;
        }

        SdkExecutors.IO.execute(() -> {
            try {
                BidRequest request = requestBuilder
                        .adFormat(AdFormat.INTERSTITIAL)
                        .adSize(AdSize.INTERSTITIAL_FULL)
                        .placementId(placementId)
                        .build();

                BidResponse response = networkClient.requestBid(request);
                BidResponse.Bid bid = response.getWinningBid();

                if (bid == null || bid.adm == null || bid.adm.isEmpty()) {
                    postToMain(() -> { if (listener != null) listener.onInterstitialFailed(new AdError.NoFill()); });
                    return;
                }

                AdData data = AdData.fromBid(request.id, bid, AdFormat.INTERSTITIAL,
                        response.cur != null ? response.cur : "USD",
                        ApexAds.getConfig().getCacheTtlSeconds());

                cache.put(AdFormat.INTERSTITIAL, placementId, data);
                adData = data;
                AdLog.i("InterstitialAd: loaded cpm=$%.2f", bid.price);
                postToMain(() -> { if (listener != null) listener.onInterstitialLoaded(); });

            } catch (Exception e) {
                AdLog.e(e, "InterstitialAd: load failed");
                postToMain(() -> {
                    if (listener != null) listener.onInterstitialFailed(new AdError.Network(e.getMessage(), e));
                });
            }
        });
    }

    /** Launches the fullscreen ad. Must be called from the main thread. */
    public void show(@NonNull Context context) {
        if (adData == null) {
            AdLog.w("InterstitialAd: show() called before ad was loaded");
            return;
        }
        if (adData.isExpired()) {
            cache.remove(AdFormat.INTERSTITIAL, placementId);
            if (listener != null) listener.onInterstitialFailed(new AdError.NoFill("Cached ad expired"));
            return;
        }
        InterstitialActivity.launch(context, adData, listener);
        adData = null;
        cache.remove(AdFormat.INTERSTITIAL, placementId);
    }

    public boolean isReady() {
        return adData != null && !adData.isExpired();
    }

    private static void postToMain(Runnable r) {
        SdkExecutors.MAIN.post(r);
    }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static final class Builder {
        private final String placementId;
        private InterstitialAdListener listener;

        public Builder(@Nullable String placementId) {
            this.placementId = placementId;
        }

        public Builder listener(@NonNull InterstitialAdListener l) { listener = l; return this; }

        @NonNull
        public InterstitialAd build() {
            if (!ApexAds.isInitialized()) {
                throw new IllegalStateException("Call ApexAds.init() before creating ad instances.");
            }
            return new InterstitialAd(this);
        }
    }
}
