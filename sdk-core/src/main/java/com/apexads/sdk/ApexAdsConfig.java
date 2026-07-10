package com.apexads.sdk;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ApexAdsConfig {

    public static final String DEFAULT_AD_SERVER_URL = BuildConfig.AD_SERVER_URL;

    public static final String DEFAULT_TRACKING_URL = BuildConfig.TRACKING_URL;
    public static final long DEFAULT_TIMEOUT_MS = 5_000L;
    public static final long DEFAULT_CACHE_TTL_SECONDS = 300L;

    private final String appToken;
    private final String adServerUrl;
    private final String trackingUrl;
    private final long requestTimeoutMs;
    private final long cacheTtlSeconds;
    private final int coppa;
    private final String gdprConsentString;
    private final String usPrivacyString;
    private final boolean debugLogging;
    private final boolean testMode;
    private final boolean debugFakeFill;
    private final String sentryDsn;
    private final String supplyChainDomain;
    private final String sellerId;

    private ApexAdsConfig(Builder builder) {
        this.appToken = builder.appToken;
        this.adServerUrl = builder.adServerUrl;
        this.trackingUrl = builder.trackingUrl;
        this.requestTimeoutMs = builder.requestTimeoutMs;
        this.cacheTtlSeconds = builder.cacheTtlSeconds;
        this.coppa = builder.coppa;
        this.gdprConsentString = builder.gdprConsentString;
        this.usPrivacyString = builder.usPrivacyString;
        this.debugLogging = builder.debugLogging;
        this.testMode = builder.testMode;
        this.debugFakeFill = builder.debugFakeFill;
        this.sentryDsn = builder.sentryDsn;
        this.supplyChainDomain = builder.supplyChainDomain;
        this.sellerId = builder.sellerId;
    }

    @NonNull public String getAppToken() { return appToken; }
    @NonNull public String getAdServerUrl() { return adServerUrl; }
    @NonNull public String getTrackingUrl() { return trackingUrl; }
    public long getRequestTimeoutMs() { return requestTimeoutMs; }
    public long getCacheTtlSeconds() { return cacheTtlSeconds; }
    public int getCoppa() { return coppa; }
    @Nullable public String getGdprConsentString() { return gdprConsentString; }
    @Nullable public String getUsPrivacyString() { return usPrivacyString; }
    public boolean isDebugLogging() { return debugLogging; }
    public boolean isTestMode() { return testMode; }

    public boolean isDebugFakeFill() { return debugFakeFill; }
    @Nullable public String getSentryDsn() { return sentryDsn; }
    @NonNull public String getSupplyChainDomain() { return supplyChainDomain; }
    @NonNull public String getSellerId() { return sellerId != null ? sellerId : appToken; }

    public static final class Builder {
        private final String appToken;
        private String adServerUrl = DEFAULT_AD_SERVER_URL;
        private String trackingUrl = DEFAULT_TRACKING_URL;
        private long requestTimeoutMs = DEFAULT_TIMEOUT_MS;
        private long cacheTtlSeconds = DEFAULT_CACHE_TTL_SECONDS;
        private int coppa = 0;
        private String gdprConsentString = null;
        private String usPrivacyString = null;
        private boolean debugLogging = false;
        private boolean testMode = false;
        private boolean debugFakeFill = false;
        private String sentryDsn = null;
        private String supplyChainDomain = "apexads.net";
        private String sellerId = null;

        public Builder(@NonNull String appToken) {
            if (appToken.trim().isEmpty()) throw new IllegalArgumentException("appToken must not be blank");
            this.appToken = appToken;
        }

        public Builder adServerUrl(@NonNull String url) { adServerUrl = url; return this; }
        public Builder trackingUrl(@NonNull String url) { trackingUrl = url; return this; }
        public Builder requestTimeoutMs(long ms) { requestTimeoutMs = ms; return this; }
        public Builder cacheTtlSeconds(long seconds) { cacheTtlSeconds = seconds; return this; }
        public Builder coppa(@IntRange(from = 0, to = 3) int value) { coppa = value; return this; }
        public Builder gdprConsentString(@Nullable String consent) { gdprConsentString = consent; return this; }
        public Builder usPrivacyString(@Nullable String privacy) { usPrivacyString = privacy; return this; }
        public Builder debugLogging(boolean enabled) { debugLogging = enabled; return this; }
        public Builder testMode(boolean enabled) { testMode = enabled; return this; }

        public Builder debugFakeFill(boolean enabled) { debugFakeFill = enabled; return this; }
        public Builder sentryDsn(@Nullable String dsn) { sentryDsn = dsn; return this; }

        /** Advertising system domain used as the SDK's SupplyChain node ({@code schain.nodes[].asi}). */
        public Builder supplyChainDomain(@NonNull String domain) { supplyChainDomain = domain; return this; }
        /** Seller identifier for the SupplyChain node ({@code sid}); defaults to the app token. */
        public Builder sellerId(@Nullable String id) { sellerId = id; return this; }

        @NonNull
        public ApexAdsConfig build() {
            return new ApexAdsConfig(this);
        }
    }
}
