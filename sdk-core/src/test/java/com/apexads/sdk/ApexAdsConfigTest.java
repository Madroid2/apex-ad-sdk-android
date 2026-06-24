package com.apexads.sdk;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class ApexAdsConfigTest {

    @Test
    public void builder_blankToken_throws() {
        assertThrows(IllegalArgumentException.class, () -> new ApexAdsConfig.Builder("  "));
    }

    @Test
    public void builder_defaults_areSafe() {
        ApexAdsConfig config = new ApexAdsConfig.Builder("token-123").build();

        assertThat(config.getAppToken()).isEqualTo("token-123");
        assertThat(config.getAdServerUrl()).isNotEmpty();
        assertThat(config.getTrackingUrl()).isNotEmpty();
        assertThat(config.getRequestTimeoutMs()).isEqualTo(ApexAdsConfig.DEFAULT_TIMEOUT_MS);
        assertThat(config.getCacheTtlSeconds()).isEqualTo(ApexAdsConfig.DEFAULT_CACHE_TTL_SECONDS);
        assertThat(config.isDebugFakeFill()).isFalse();
        assertThat(config.isTestMode()).isFalse();
        assertThat(config.getSentryDsn()).isNull();
    }

    @Test
    public void builder_overrides_allFields() {
        ApexAdsConfig config = new ApexAdsConfig.Builder("token-123")
                .adServerUrl("https://dsp.example.com/openrtb")
                .trackingUrl("https://track.example.com")
                .requestTimeoutMs(1234L)
                .cacheTtlSeconds(42L)
                .coppa(1)
                .gdprConsentString("TCF")
                .usPrivacyString("1YNN")
                .debugLogging(true)
                .testMode(true)
                .debugFakeFill(true)
                .sentryDsn("https://sentry.example/1")
                .build();

        assertThat(config.getAdServerUrl()).isEqualTo("https://dsp.example.com/openrtb");
        assertThat(config.getTrackingUrl()).isEqualTo("https://track.example.com");
        assertThat(config.getRequestTimeoutMs()).isEqualTo(1234L);
        assertThat(config.getCacheTtlSeconds()).isEqualTo(42L);
        assertThat(config.getCoppa()).isEqualTo(1);
        assertThat(config.getGdprConsentString()).isEqualTo("TCF");
        assertThat(config.getUsPrivacyString()).isEqualTo("1YNN");
        assertThat(config.isDebugLogging()).isTrue();
        assertThat(config.isTestMode()).isTrue();
        assertThat(config.isDebugFakeFill()).isTrue();
        assertThat(config.getSentryDsn()).isEqualTo("https://sentry.example/1");
    }
}
