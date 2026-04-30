package com.apexads.sdk.banner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.ApexAds;
import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.network.AdNetworkClient;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.request.OpenRTBRequestBuilder;
import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;

import com.apexads.sdk.core.utils.AdLog;

/**
 * Publisher-facing banner ad controller.
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

    private final String placementId;
    private final AdSize adSize;
    private final double bidFloor;
    private final BannerAdListener listener;
    private final AdNetworkClient networkClient;
    private final AdCache cache;
    private final OpenRTBRequestBuilder requestBuilder;

    private volatile AdData adData;

    private BannerAd(Builder builder) {
        this.placementId   = builder.placementId;
        this.adSize        = builder.adSize;
        this.bidFloor      = builder.bidFloor;
        this.listener      = builder.listener;
        this.networkClient = ApexAds.getNetworkClient();
        this.cache         = new AdCache();
        this.requestBuilder = new OpenRTBRequestBuilder(
                ApexAds.getDeviceInfoProvider(), ApexAds.getConsentManager());
    }

    /** Fetches an ad from the exchange (or serves from cache). Non-blocking. */
    public void load() {
        AdData cached = cache.get(AdFormat.BANNER, placementId);
        if (cached != null) {
            AdLog.d("BannerAd: serving from cache");
            adData = cached;
            postToMain(() -> { if (listener != null) listener.onAdLoaded(); });
            return;
        }

        SdkExecutors.IO.execute(() -> {
            try {
                BidRequest request = requestBuilder
                        .adFormat(AdFormat.BANNER)
                        .adSize(adSize)
                        .placementId(placementId)
                        .bidFloor(bidFloor)
                        .build();

                BidResponse response = networkClient.requestBid(request);
                BidResponse.Bid bid = response.getWinningBid();

                if (bid == null || bid.adm == null || bid.adm.isEmpty()) {
                    postToMain(() -> { if (listener != null) listener.onAdFailed(new AdError.NoFill()); });
                    return;
                }

                AdData data = AdData.fromBid(request.id, bid, AdFormat.BANNER,
                        response.cur != null ? response.cur : "USD",
                        ApexAds.getConfig().getCacheTtlSeconds());

                cache.put(AdFormat.BANNER, placementId, data);
                adData = data;
                AdLog.i("BannerAd: loaded cpm=$%.2f creative=%s", bid.price, bid.crid);
                postToMain(() -> { if (listener != null) listener.onAdLoaded(); });

            } catch (Exception e) {
                AdLog.e(e, "BannerAd: load failed");
                postToMain(() -> {
                    if (listener != null) listener.onAdFailed(new AdError.Network(e.getMessage(), e));
                });
            }
        });
    }

    /** Renders the loaded ad into {@code view}. Call after {@link BannerAdListener#onAdLoaded()}. */
    public void show(@NonNull BannerAdView view) {
        if (adData == null) {
            AdLog.w("BannerAd: show() called before ad was loaded");
            return;
        }
        if (adData.isExpired()) {
            cache.remove(AdFormat.BANNER, placementId);
            if (listener != null) listener.onAdFailed(new AdError.NoFill("Cached ad expired"));
            return;
        }
        view.setListener(listener);
        view.render(adData);
    }

    public void destroy() {
        cache.remove(AdFormat.BANNER, placementId);
        adData = null;
    }

    private static void postToMain(Runnable r) {
        SdkExecutors.MAIN.post(r);
    }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static final class Builder {
        private final String placementId;
        private AdSize adSize = AdSize.BANNER_320x50;
        private double bidFloor = 0.0;
        private BannerAdListener listener;

        public Builder(@Nullable String placementId) {
            this.placementId = placementId;
        }

        public Builder adSize(@NonNull AdSize size) { adSize = size; return this; }
        public Builder bidFloor(double floor) { bidFloor = floor; return this; }
        public Builder listener(@NonNull BannerAdListener l) { listener = l; return this; }

        @NonNull
        public BannerAd build() {
            if (!ApexAds.isInitialized()) {
                throw new IllegalStateException("Call ApexAds.init() before creating ad instances.");
            }
            return new BannerAd(this);
        }
    }
}
