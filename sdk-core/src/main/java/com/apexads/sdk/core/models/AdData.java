package com.apexads.sdk.core.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.models.openrtb.BidResponse;

/**
 * Resolved ad data passed from the auction engine to the renderer.
 *
 * Kept as a plain Java class (not Parcelable) — inter-Activity communication
 * uses a static holder in each Activity so we avoid the verbose Parcelable
 * boilerplate and any cross-process concerns (SDK activities are internal only).
 */
public final class AdData {

    public final String requestId;
    public final String impressionId;
    public final String bidId;
    public final String adMarkup;
    @Nullable public final String winNoticeUrl;
    @Nullable public final String creativeId;
    public final AdFormat adFormat;
    public final int width;
    public final int height;
    public final double cpm;
    public final String currency;
    public final long expiresAt;          // epoch ms
    @Nullable public final NativeAdPayload nativePayload;
    @Nullable public final String vastXml;

    private AdData(Builder b) {
        requestId = b.requestId;
        impressionId = b.impressionId;
        bidId = b.bidId;
        adMarkup = b.adMarkup;
        winNoticeUrl = b.winNoticeUrl;
        creativeId = b.creativeId;
        adFormat = b.adFormat;
        width = b.width;
        height = b.height;
        cpm = b.cpm;
        currency = b.currency;
        expiresAt = b.expiresAt;
        nativePayload = b.nativePayload;
        vastXml = b.vastXml;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    @NonNull
    public static AdData fromBid(@NonNull String requestId,
                                 @NonNull BidResponse.Bid bid,
                                 @NonNull AdFormat format,
                                 @NonNull String currency,
                                 long ttlSeconds) {
        boolean isVideo = format == AdFormat.REWARDED_VIDEO;
        return new Builder()
                .requestId(requestId)
                .impressionId(bid.impid)
                .bidId(bid.id)
                .adMarkup(bid.adm != null ? bid.adm : "")
                .winNoticeUrl(bid.nurl)
                .creativeId(bid.crid)
                .adFormat(format)
                .width(bid.w != null ? bid.w : 0)
                .height(bid.h != null ? bid.h : 0)
                .cpm(bid.price)
                .currency(currency)
                .expiresAt(System.currentTimeMillis() + ttlSeconds * 1000L)
                .vastXml(isVideo ? bid.adm : null)
                .build();
    }

    public AdData withNativePayload(@NonNull NativeAdPayload payload) {
        return new Builder()
                .requestId(requestId).impressionId(impressionId).bidId(bidId)
                .adMarkup(adMarkup).winNoticeUrl(winNoticeUrl).creativeId(creativeId)
                .adFormat(adFormat).width(width).height(height).cpm(cpm)
                .currency(currency).expiresAt(expiresAt).vastXml(vastXml)
                .nativePayload(payload)
                .build();
    }

    public static final class Builder {
        String requestId, impressionId, bidId, adMarkup = "", currency = "USD";
        String winNoticeUrl, creativeId, vastXml;
        AdFormat adFormat;
        int width, height;
        double cpm;
        long expiresAt;
        NativeAdPayload nativePayload;

        public Builder requestId(String v) { requestId = v; return this; }
        public Builder impressionId(String v) { impressionId = v; return this; }
        public Builder bidId(String v) { bidId = v; return this; }
        public Builder adMarkup(String v) { adMarkup = v; return this; }
        public Builder winNoticeUrl(String v) { winNoticeUrl = v; return this; }
        public Builder creativeId(String v) { creativeId = v; return this; }
        public Builder adFormat(AdFormat v) { adFormat = v; return this; }
        public Builder width(int v) { width = v; return this; }
        public Builder height(int v) { height = v; return this; }
        public Builder cpm(double v) { cpm = v; return this; }
        public Builder currency(String v) { currency = v; return this; }
        public Builder expiresAt(long v) { expiresAt = v; return this; }
        public Builder vastXml(String v) { vastXml = v; return this; }
        public Builder nativePayload(NativeAdPayload v) { nativePayload = v; return this; }

        public AdData build() { return new AdData(this); }
    }
}
