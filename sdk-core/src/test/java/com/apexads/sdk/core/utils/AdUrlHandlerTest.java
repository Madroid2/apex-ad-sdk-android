package com.apexads.sdk.core.utils;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class AdUrlHandlerTest {

    @Test
    public void normalizeExternalWebUrl_allowsHttpAndHttpsWithHost() {
        assertThat(AdUrlHandler.normalizeExternalWebUrl("https://example.com/path?q=1"))
                .isEqualTo("https://example.com/path?q=1");
        assertThat(AdUrlHandler.normalizeExternalWebUrl("  http://example.com  "))
                .isEqualTo("http://example.com");
        assertThat(AdUrlHandler.normalizeExternalWebUrl("https://8.8.8.8/click"))
                .isEqualTo("https://8.8.8.8/click");
    }

    @Test
    public void normalizeExternalWebUrl_rejectsUnsafeSchemes() {
        assertThat(AdUrlHandler.normalizeExternalWebUrl("javascript:alert(1)")).isNull();
        assertThat(AdUrlHandler.normalizeExternalWebUrl("file:///sdcard/private.txt")).isNull();
        assertThat(AdUrlHandler.normalizeExternalWebUrl("content://com.example/private")).isNull();
        assertThat(AdUrlHandler.normalizeExternalWebUrl("intent://scan/#Intent;scheme=zxing;end")).isNull();
        assertThat(AdUrlHandler.normalizeExternalWebUrl("data:text/html,<script>alert(1)</script>")).isNull();
    }

    @Test
    public void normalizeExternalWebUrl_rejectsMalformedAndHostlessUrls() {
        assertThat(AdUrlHandler.normalizeExternalWebUrl(null)).isNull();
        assertThat(AdUrlHandler.normalizeExternalWebUrl("")).isNull();
        assertThat(AdUrlHandler.normalizeExternalWebUrl("/relative/path")).isNull();
        assertThat(AdUrlHandler.normalizeExternalWebUrl("https:///missing-host")).isNull();
        assertThat(AdUrlHandler.normalizeExternalWebUrl("https://exa mple.com")).isNull();
        assertThat(AdUrlHandler.normalizeExternalWebUrl("https://example.com/\nnext")).isNull();
    }

    @Test
    public void normalizeExternalWebUrl_rejectsLocalAndPrivateHosts() {
        assertThat(AdUrlHandler.normalizeExternalWebUrl("https://localhost/click")).isNull();
        assertThat(AdUrlHandler.normalizeExternalWebUrl("https://ad.local/click")).isNull();
        assertThat(AdUrlHandler.normalizeExternalWebUrl("http://127.0.0.1/click")).isNull();
        assertThat(AdUrlHandler.normalizeExternalWebUrl("http://10.0.2.2/click")).isNull();
        assertThat(AdUrlHandler.normalizeExternalWebUrl("http://172.16.0.1/click")).isNull();
        assertThat(AdUrlHandler.normalizeExternalWebUrl("http://192.168.1.1/click")).isNull();
        assertThat(AdUrlHandler.normalizeExternalWebUrl("http://[::1]/click")).isNull();
        assertThat(AdUrlHandler.normalizeExternalWebUrl("http://2130706433/click")).isNull();
        assertThat(AdUrlHandler.normalizeExternalWebUrl("http://0177.0.0.1/click")).isNull();
        assertThat(AdUrlHandler.normalizeExternalWebUrl("http://[::ffff:127.0.0.1]/click")).isNull();
    }
}
