package com.apexads.sdk.nativeads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.ApexAds;
import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.NativeAdPayload;
import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;
import com.apexads.sdk.core.network.AdNetworkClient;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.request.OpenRTBRequestBuilder;

import com.apexads.sdk.core.utils.AdLog;

/**
 * Native ad (IAB OpenRTB Native 1.2).
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

    private final String placementId;
    private final NativeAdListener listener;
    private final AdNetworkClient networkClient;
    private final AdCache cache;
    private final OpenRTBRequestBuilder requestBuilder;
    private final NativeAdParser parser;

    private volatile NativeAdPayload payload;

    private NativeAd(Builder builder) {
        this.placementId    = builder.placementId;
        this.listener       = builder.listener;
        this.networkClient  = ApexAds.getNetworkClient();
        this.cache          = new AdCache();
        this.requestBuilder = new OpenRTBRequestBuilder(
                ApexAds.getDeviceInfoProvider(), ApexAds.getConsentManager());
        this.parser         = new NativeAdParser();
    }

    public void load() {
        AdData cached = cache.get(AdFormat.NATIVE, placementId);
        if (cached != null && cached.nativePayload != null) {
            payload = cached.nativePayload;
            postToMain(() -> { if (listener != null) listener.onNativeAdLoaded(this); });
            return;
        }

        SdkExecutors.IO.execute(() -> {
            try {
                BidRequest request = requestBuilder
                        .adFormat(AdFormat.NATIVE)
                        .placementId(placementId)
                        .build();

                BidResponse response = networkClient.requestBid(request);
                BidResponse.Bid bid  = response.getWinningBid();

                if (bid == null || bid.adm == null || bid.adm.isEmpty()) {
                    postToMain(() -> { if (listener != null) listener.onNativeAdFailed(new AdError.NoFill()); });
                    return;
                }

                NativeAdPayload parsed = parser.parse(bid.adm);
                if (parsed == null) {
                    postToMain(() -> {
                        if (listener != null) listener.onNativeAdFailed(
                            new AdError.InvalidMarkup("Native JSON parse failed"));
                    });
                    return;
                }

                AdData data = AdData.fromBid(request.id, bid, AdFormat.NATIVE,
                        response.cur != null ? response.cur : "USD",
                        ApexAds.getConfig().getCacheTtlSeconds())
                        .withNativePayload(parsed);

                cache.put(AdFormat.NATIVE, placementId, data);
                payload = parsed;
                AdLog.i("NativeAd: loaded title='%s'", parsed.title);
                postToMain(() -> { if (listener != null) listener.onNativeAdLoaded(this); });

            } catch (Exception e) {
                AdLog.e(e, "NativeAd: load failed");
                postToMain(() -> {
                    if (listener != null) listener.onNativeAdFailed(new AdError.Network(e.getMessage(), e));
                });
            }
        });
    }

    /** Binds loaded ad assets to {@code view}. Call after {@link NativeAdListener#onNativeAdLoaded}. */
    public void bindTo(@NonNull NativeAdView view) {
        if (payload == null) {
            AdLog.w("NativeAd: bindTo() called before ad was loaded");
            return;
        }
        view.bind(payload, networkClient);
    }

    @Nullable public String getTitle()           { return payload != null ? payload.title : null; }
    @Nullable public String getDescription()   { return payload != null ? payload.description : null; }
    @Nullable public String getCtaText()       { return payload != null ? payload.ctaText : null; }
    @Nullable public String getIconUrl()       { return payload != null ? payload.iconUrl : null; }
    @Nullable public String getImageUrl()      { return payload != null ? payload.imageUrl : null; }
    @Nullable public String getAdvertiserName(){ return payload != null ? payload.advertiserName : null; }

    private static void postToMain(Runnable r) { SdkExecutors.MAIN.post(r); }

    // ── Builder ──────────────────────────────────────────────────────────────

    public static final class Builder {
        private final String placementId;
        private NativeAdListener listener;

        public Builder(@Nullable String placementId) { this.placementId = placementId; }

        public Builder listener(@NonNull NativeAdListener l) { listener = l; return this; }

        @NonNull
        public NativeAd build() {
            if (!ApexAds.isInitialized()) {
                throw new IllegalStateException("Call ApexAds.init() before creating ad instances.");
            }
            return new NativeAd(this);
        }
    }
}
