package com.apexads.sdk.core.utils;

import static com.google.common.truth.Truth.assertThat;

import org.junit.After;
import org.junit.Test;

public class AdUrlHandlerTest {

    @After
    public void restoreLocalHostPolicy() {
        AdUrlHandler.allowLocalHosts = com.apexads.sdk.BuildConfig.DEBUG;
    }

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
    public void normalizeDeeplink_allowsCustomSchemesAndMarket() {
        assertThat(AdUrlHandler.normalizeDeeplink("myapp://product/42"))
                .isEqualTo("myapp://product/42");
        assertThat(AdUrlHandler.normalizeDeeplink("  market://details?id=com.example.app  "))
                .isEqualTo("market://details?id=com.example.app");
        assertThat(AdUrlHandler.normalizeDeeplink("fb://profile/12345"))
                .isEqualTo("fb://profile/12345");
    }

    @Test
    public void normalizeDeeplink_rejectsWebAndDangerousSchemes() {
        // Web URLs take the normalizeExternalWebUrl path, not the deep-link path.
        assertThat(AdUrlHandler.normalizeDeeplink("https://example.com")).isNull();
        assertThat(AdUrlHandler.normalizeDeeplink("http://example.com")).isNull();
        // Dangerous schemes are blocked outright.
        assertThat(AdUrlHandler.normalizeDeeplink("intent://scan/#Intent;scheme=zxing;end")).isNull();
        assertThat(AdUrlHandler.normalizeDeeplink("javascript:alert(1)")).isNull();
        assertThat(AdUrlHandler.normalizeDeeplink("file:///sdcard/private.txt")).isNull();
        assertThat(AdUrlHandler.normalizeDeeplink("content://com.example/private")).isNull();
        assertThat(AdUrlHandler.normalizeDeeplink("data:text/html,x")).isNull();
        assertThat(AdUrlHandler.normalizeDeeplink("android-app://com.example")).isNull();
        // Scheme casing must not bypass the block list.
        assertThat(AdUrlHandler.normalizeDeeplink("INTENT://scan/#Intent;end")).isNull();
        assertThat(AdUrlHandler.normalizeDeeplink("JavaScript:alert(1)")).isNull();
    }

    @Test
    public void normalizeDeeplink_rejectsMalformedOrEmptyDestinations() {
        assertThat(AdUrlHandler.normalizeDeeplink(null)).isNull();
        assertThat(AdUrlHandler.normalizeDeeplink("")).isNull();
        assertThat(AdUrlHandler.normalizeDeeplink("myapp://")).isNull();
        assertThat(AdUrlHandler.normalizeDeeplink("noscheme/path")).isNull();
        assertThat(AdUrlHandler.normalizeDeeplink("myapp://line\nbreak")).isNull();
    }

    @Test
    public void marketToPlayStoreWebUrl_convertsMarketLinks() {
        assertThat(AdUrlHandler.marketToPlayStoreWebUrl("market://details?id=com.example.app"))
                .isEqualTo("https://play.google.com/store/apps/details?id=com.example.app");
        assertThat(AdUrlHandler.marketToPlayStoreWebUrl(
                "market://details?id=com.example.app&referrer=utm_source%3Dapex"))
                .isEqualTo("https://play.google.com/store/apps/details?id=com.example.app&referrer=utm_source%3Dapex");
    }

    @Test
    public void marketToPlayStoreWebUrl_rejectsNonMarketOrQuerylessLinks() {
        assertThat(AdUrlHandler.marketToPlayStoreWebUrl("myapp://details?id=x")).isNull();
        assertThat(AdUrlHandler.marketToPlayStoreWebUrl("market://details")).isNull();
        assertThat(AdUrlHandler.marketToPlayStoreWebUrl(null)).isNull();
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
        // This asserts the production (release) SSRF protection; debug builds
        // intentionally relax it, so pin strict mode for this test.
        AdUrlHandler.allowLocalHosts = false;
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

    @Test
    public void normalizeExternalWebUrl_allowsPrivateHostsWhenDebugRelaxationOn() {
        // Debug builds relax the private-host block so the local demand platform's
        // click-tracking redirects (private LAN IP) work end-to-end.
        AdUrlHandler.allowLocalHosts = true;
        assertThat(AdUrlHandler.normalizeExternalWebUrl("http://192.168.2.108:3000/api/track/click?redirect=x"))
                .isEqualTo("http://192.168.2.108:3000/api/track/click?redirect=x");
        // Scheme / malformed checks still apply even with the relaxation on.
        assertThat(AdUrlHandler.normalizeExternalWebUrl("javascript:alert(1)")).isNull();
    }
}
