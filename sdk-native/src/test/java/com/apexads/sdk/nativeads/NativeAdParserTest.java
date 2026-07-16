package com.apexads.sdk.nativeads;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.apexads.sdk.core.models.NativeAdPayload;

import org.junit.Test;

public class NativeAdParserTest {

    private final NativeAdParser parser = new NativeAdParser();

    @Test
    public void parse_nullBlankMalformedAndMissingTitle_returnNull() {
        assertThat(parser.parse(null)).isNull();
        assertThat(parser.parse("  ")).isNull();
        assertThat(parser.parse("{bad-json")).isNull();
        assertThat(parser.parse("{\"native\":{\"assets\":[]}}")).isNull();
    }

    @Test
    public void parse_nativeWrapper_extractsAllAssetsAndTrackers() {
        NativeAdPayload payload = parser.parse("{\"native\":{\"link\":{\"url\":\"https://click\"},"
                + "\"imptrackers\":[\"https://imp1\",\"https://imp2\"],"
                + "\"assets\":["
                + "{\"id\":1,\"title\":{\"text\":\"Holiday Sale\"}},"
                + "{\"id\":2,\"img\":{\"url\":\"https://img\"}},"
                + "{\"id\":3,\"img\":{\"url\":\"https://icon\"}},"
                + "{\"id\":4,\"data\":{\"value\":\"Save more today\"}},"
                + "{\"id\":5,\"data\":{\"value\":\"Apex Store\"}},"
                + "{\"id\":6,\"data\":{\"value\":\"Shop Now\"}}"
                + "]}}");

        assertThat(payload).isNotNull();
        assertThat(payload.title).isEqualTo("Holiday Sale");
        assertThat(payload.description).isEqualTo("Save more today");
        assertThat(payload.imageUrl).isEqualTo("https://img");
        assertThat(payload.iconUrl).isEqualTo("https://icon");
        assertThat(payload.advertiserName).isEqualTo("Apex Store");
        assertThat(payload.ctaText).isEqualTo("Shop Now");
        assertThat(payload.clickUrl).isEqualTo("https://click");
        assertThat(payload.impressionTrackers).containsExactly("https://imp1", "https://imp2").inOrder();
        // No deep-link fields in this markup.
        assertThat(payload.fallbackUrl).isNull();
        assertThat(payload.clickTrackers).isEmpty();
    }

    @Test
    public void parse_linkWithDeeplinkFallbackAndClicktrackers_extractsAll() {
        NativeAdPayload payload = parser.parse("{\"native\":{\"link\":{"
                + "\"url\":\"myapp://product/42\","
                + "\"fallback\":\"https://example.com/landing\","
                + "\"clicktrackers\":[\"https://track/click1\",\"https://track/click2\"]},"
                + "\"assets\":[{\"id\":1,\"title\":{\"text\":\"T\"}}]}}");

        assertThat(payload).isNotNull();
        assertThat(payload.clickUrl).isEqualTo("myapp://product/42");
        assertThat(payload.fallbackUrl).isEqualTo("https://example.com/landing");
        assertThat(payload.clickTrackers)
                .containsExactly("https://track/click1", "https://track/click2").inOrder();
    }

    @Test
    public void parse_emptyFallback_normalizesToNull() {
        NativeAdPayload payload = parser.parse("{\"native\":{\"link\":{"
                + "\"url\":\"https://click\",\"fallback\":\"\"},"
                + "\"assets\":[{\"id\":1,\"title\":{\"text\":\"T\"}}]}}");

        assertThat(payload).isNotNull();
        assertThat(payload.fallbackUrl).isNull();
    }

    @Test
    public void parse_directNative_defaultsOptionalFields() {
        NativeAdPayload payload = parser.parse("{\"link\":{\"url\":\"\"},"
                + "\"assets\":[{\"id\":1,\"title\":{\"text\":\"Title\"}}]}");

        assertThat(payload).isNotNull();
        assertThat(payload.title).isEqualTo("Title");
        assertThat(payload.description).isEmpty();
        assertThat(payload.ctaText).isEqualTo("Learn More");
        assertThat(payload.clickUrl).isNull();
        assertThat(payload.impressionTrackers).isEmpty();
    }

    @Test
    public void parse_impressionTrackers_areImmutable() {
        NativeAdPayload payload = parser.parse("{\"imptrackers\":[\"https://imp\"],"
                + "\"assets\":[{\"id\":1,\"title\":{\"text\":\"Title\"}}]}");

        assertThrows(UnsupportedOperationException.class,
                () -> payload.impressionTrackers.add("https://extra"));
    }
}
