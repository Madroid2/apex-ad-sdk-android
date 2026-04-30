package com.apexads.sdk.video;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.ApexAds;
import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;
import com.apexads.sdk.core.network.AdNetworkClient;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.request.OpenRTBRequestBuilder;
import com.apexads.sdk.video.vast.VastParser;

import com.apexads.sdk.core.utils.AdLog;

/**
 * Rewarded / pre-roll video ad backed by VAST 4.0.
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

    private final String placementId;
    private final VideoAdListener listener;
    private final AdNetworkClient networkClient;
    private final AdCache cache;
    private final OpenRTBRequestBuilder requestBuilder;
    private final VastParser vastParser;

    private volatile VastParser.VastAd vastAd;
    private volatile AdData adData;

    private VideoAd(Builder builder) {
        this.placementId    = builder.placementId;
        this.listener       = builder.listener;
        this.networkClient  = ApexAds.getNetworkClient();
        this.cache          = new AdCache();
        this.requestBuilder = new OpenRTBRequestBuilder(
                ApexAds.getDeviceInfoProvider(), ApexAds.getConsentManager());
        this.vastParser     = new VastParser();
    }

    public void load() {
        AdData cached = cache.get(AdFormat.REWARDED_VIDEO, placementId);
        if (cached != null && cached.vastXml != null && !cached.isExpired()) {
            VastParser.VastResult result = vastParser.parse(cached.vastXml);
            if (result.isSuccess()) {
                vastAd = result.ad;
                adData = cached;
                postToMain(() -> { if (listener != null) listener.onVideoAdLoaded(); });
                return;
            }
        }

        SdkExecutors.IO.execute(() -> {
            try {
                BidRequest request = requestBuilder
                        .adFormat(AdFormat.REWARDED_VIDEO)
                        .placementId(placementId)
                        .build();

                BidResponse response = networkClient.requestBid(request);
                BidResponse.Bid bid  = response.getWinningBid();

                if (bid == null || bid.adm == null || bid.adm.isEmpty()) {
                    postToMain(() -> { if (listener != null) listener.onVideoAdFailed(new AdError.NoFill()); });
                    return;
                }

                VastParser.VastResult result = vastParser.parse(bid.adm);
                if (!result.isSuccess()) {
                    String msg = result.isNoFill ? "No fill" : result.errorMessage;
                    postToMain(() -> {
                        if (listener != null) listener.onVideoAdFailed(new AdError.InvalidMarkup(msg));
                    });
                    return;
                }

                // fromBid sets vastXml automatically for REWARDED_VIDEO format
                AdData data = AdData.fromBid(request.id, bid, AdFormat.REWARDED_VIDEO,
                        response.cur != null ? response.cur : "USD",
                        ApexAds.getConfig().getCacheTtlSeconds());

                cache.put(AdFormat.REWARDED_VIDEO, placementId, data);
                vastAd = result.ad;
                adData = data;
                AdLog.i("VideoAd: loaded adId=%s duration=%ds", result.ad.adId, result.ad.duration);
                postToMain(() -> { if (listener != null) listener.onVideoAdLoaded(); });

            } catch (Exception e) {
                AdLog.e(e, "VideoAd: load failed");
                postToMain(() -> {
                    if (listener != null) listener.onVideoAdFailed(new AdError.Network(e.getMessage(), e));
                });
            }
        });
    }

    /** Launches {@link VideoAdActivity}. Call only after {@link VideoAdListener#onVideoAdLoaded}. */
    public void show(@NonNull Context context) {
        VastParser.VastAd ad = vastAd;
        if (ad == null) {
            AdLog.w("VideoAd: show() called before ad was loaded");
            return;
        }
        VideoAdActivity.launch(context, ad, networkClient, listener);
    }

    public boolean isReady() {
        return vastAd != null && adData != null && !adData.isExpired();
    }

    private static void postToMain(Runnable r) { SdkExecutors.MAIN.post(r); }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static final class Builder {
        private final String placementId;
        private VideoAdListener listener;

        public Builder(@Nullable String placementId) { this.placementId = placementId; }

        public Builder listener(@NonNull VideoAdListener l) { listener = l; return this; }

        @NonNull
        public VideoAd build() {
            if (!ApexAds.isInitialized()) {
                throw new IllegalStateException("Call ApexAds.init() before creating ad instances.");
            }
            return new VideoAd(this);
        }
    }
}
