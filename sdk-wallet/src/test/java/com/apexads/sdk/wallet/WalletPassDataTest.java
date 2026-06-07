package com.apexads.sdk.wallet;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class WalletPassDataTest {

    @Test
    public void fromJson_absentMalformedOrMissingJwt_returnsNull() {
        assertThat(WalletPassData.fromJson(null)).isNull();
        assertThat(WalletPassData.fromJson("")).isNull();
        assertThat(WalletPassData.fromJson("{bad-json")).isNull();
        assertThat(WalletPassData.fromJson("{\"offer_id\":\"offer\"}")).isNull();
        assertThat(WalletPassData.fromJson("{\"pass_jwt\":\"\"}")).isNull();
    }

    @Test
    public void fromJson_validPayload_extractsFields() {
        WalletPassData data = WalletPassData.fromJson("{"
                + "\"pass_jwt\":\"signed.jwt\","
                + "\"pass_type\":\"loyalty\","
                + "\"offer_id\":\"offer-123\","
                + "\"save_tracking_url\":\"https://track\","
                + "\"cta_text\":\"Save Offer\""
                + "}");

        assertThat(data).isNotNull();
        assertThat(data.passJwt).isEqualTo("signed.jwt");
        assertThat(data.passType).isEqualTo("loyalty");
        assertThat(data.offerId).isEqualTo("offer-123");
        assertThat(data.saveTrackingUrl).isEqualTo("https://track");
        assertThat(data.ctaText).isEqualTo("Save Offer");
    }

    @Test
    public void fromJson_missingOptionalFields_usesSafeDefaults() {
        WalletPassData data = WalletPassData.fromJson("{\"pass_jwt\":\"signed.jwt\"}");

        assertThat(data).isNotNull();
        assertThat(data.passType).isEqualTo("coupon");
        assertThat(data.offerId).isEmpty();
        assertThat(data.saveTrackingUrl).isNull();
        assertThat(data.ctaText).isEqualTo("Save to Google Wallet");
    }

    @Test
    public void toString_doesNotLeakJwt() {
        WalletPassData data = WalletPassData.fromJson("{"
                + "\"pass_jwt\":\"secret.jwt\","
                + "\"pass_type\":\"coupon\","
                + "\"offer_id\":\"offer-123\""
                + "}");

        assertThat(data.toString()).contains("offer-123");
        assertThat(data.toString()).doesNotContain("secret.jwt");
    }
}
