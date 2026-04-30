package com.apexads.sdk.video.vast;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class VastParserTest {

    private VastParser parser;

    @Before
    public void setUp() {
        parser = new VastParser();
    }

    // ── Null / empty input ────────────────────────────────────────────────────

    @Test
    public void parse_nullXml_returnsError() {
        VastParser.VastResult result = parser.parse(null);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isNoFill).isFalse();
        assertThat(result.errorMessage).isNotEmpty();
    }

    @Test
    public void parse_emptyXml_returnsError() {
        VastParser.VastResult result = parser.parse("   ");
        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    public void parse_vastWithNoAds_returnsNoFill() {
        String xml = "<VAST version=\"4.0\"></VAST>";
        VastParser.VastResult result = parser.parse(xml);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isNoFill).isTrue();
    }

    // ── Inline ad ─────────────────────────────────────────────────────────────

    @Test
    public void parse_validInlineAd_succeeds() {
        VastParser.VastResult result = parser.parse(INLINE_VAST);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.ad).isNotNull();
    }

    @Test
    public void parse_inlineAd_adIdAndTitleParsed() {
        VastParser.VastAd ad = parser.parse(INLINE_VAST).ad;
        assertThat(ad.adId).isEqualTo("test-ad-001");
        assertThat(ad.adTitle).isEqualTo("Test Video Ad");
    }

    @Test
    public void parse_inlineAd_durationParsed() {
        VastParser.VastAd ad = parser.parse(INLINE_VAST).ad;
        assertThat(ad.duration).isEqualTo(15); // 00:00:15
    }

    @Test
    public void parse_inlineAd_mediaFileParsed() {
        VastParser.VastAd ad = parser.parse(INLINE_VAST).ad;
        assertThat(ad.mediaFiles).hasSize(1);

        VastParser.MediaFile mf = ad.mediaFiles.get(0);
        assertThat(mf.url).isEqualTo("https://example.com/ad.mp4");
        assertThat(mf.type).isEqualTo("video/mp4");
        assertThat(mf.width).isEqualTo(640);
        assertThat(mf.height).isEqualTo(360);
        assertThat(mf.bitrate).isEqualTo(500);
    }

    @Test
    public void parse_inlineAd_trackingEventsParsed() {
        VastParser.VastAd ad = parser.parse(INLINE_VAST).ad;
        List<String> startUrls = ad.trackingEvents.get(VastParser.TrackingEvent.START);
        assertThat(startUrls).isNotNull();
        assertThat(startUrls).containsExactly("https://track.example.com/start");

        List<String> completeUrls = ad.trackingEvents.get(VastParser.TrackingEvent.COMPLETE);
        assertThat(completeUrls).containsExactly("https://track.example.com/complete");
    }

    @Test
    public void parse_inlineAd_clickThroughParsed() {
        VastParser.VastAd ad = parser.parse(INLINE_VAST).ad;
        assertThat(ad.clickThroughUrl).isEqualTo("https://example.com/click");
    }

    @Test
    public void parse_inlineAd_impressionUrlsParsed() {
        VastParser.VastAd ad = parser.parse(INLINE_VAST).ad;
        assertThat(ad.impressionUrls).containsExactly("https://track.example.com/impression");
    }

    @Test
    public void parse_inlineAd_skipOffsetParsed() {
        VastParser.VastAd ad = parser.parse(INLINE_VAST).ad;
        assertThat(ad.skipOffset).isEqualTo(5); // skipoffset="5"
    }

    @Test
    public void parse_inlineAd_notSkippable_returnsMinusOne() {
        VastParser.VastAd ad = parser.parse(INLINE_VAST_NO_SKIP).ad;
        assertThat(ad.skipOffset).isEqualTo(-1);
    }

    // ── getBestMediaFile ──────────────────────────────────────────────────────

    @Test
    public void getBestMediaFile_returnsHighestBitrateMP4() {
        VastParser.VastAd ad = parser.parse(MULTI_MEDIA_VAST).ad;
        VastParser.MediaFile best = ad.getBestMediaFile();
        assertThat(best).isNotNull();
        assertThat(best.bitrate).isEqualTo(2000);
        assertThat(best.type).isEqualTo("video/mp4");
    }

    @Test
    public void getBestMediaFile_noMp4_returnsNull() {
        VastParser.VastAd ad = parser.parse(WEBM_ONLY_VAST).ad;
        assertThat(ad.getBestMediaFile()).isNull();
    }

    // ── Wrapper ───────────────────────────────────────────────────────────────

    @Test
    public void parse_wrapperAd_isWrapperTrue() {
        VastParser.VastResult result = parser.parse(WRAPPER_VAST);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.ad.isWrapper).isTrue();
        assertThat(result.ad.wrapperAdTagUri).isEqualTo("https://example.com/wrapper-tag.xml");
    }

    // ── TrackingEvent.fromVastName ────────────────────────────────────────────

    @Test
    public void fromVastName_knownEvents_parsedCorrectly() {
        assertThat(VastParser.TrackingEvent.fromVastName("start")).isEqualTo(VastParser.TrackingEvent.START);
        assertThat(VastParser.TrackingEvent.fromVastName("firstQuartile")).isEqualTo(VastParser.TrackingEvent.FIRST_QUARTILE);
        assertThat(VastParser.TrackingEvent.fromVastName("midpoint")).isEqualTo(VastParser.TrackingEvent.MIDPOINT);
        assertThat(VastParser.TrackingEvent.fromVastName("thirdQuartile")).isEqualTo(VastParser.TrackingEvent.THIRD_QUARTILE);
        assertThat(VastParser.TrackingEvent.fromVastName("complete")).isEqualTo(VastParser.TrackingEvent.COMPLETE);
        assertThat(VastParser.TrackingEvent.fromVastName("skip")).isEqualTo(VastParser.TrackingEvent.SKIP);
    }

    @Test
    public void fromVastName_unknownEvent_returnsNull() {
        assertThat(VastParser.TrackingEvent.fromVastName("unknownEvent")).isNull();
        assertThat(VastParser.TrackingEvent.fromVastName("")).isNull();
    }

    @Test
    public void fromVastName_caseInsensitive() {
        assertThat(VastParser.TrackingEvent.fromVastName("START")).isEqualTo(VastParser.TrackingEvent.START);
        assertThat(VastParser.TrackingEvent.fromVastName("Complete")).isEqualTo(VastParser.TrackingEvent.COMPLETE);
    }

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private static final String INLINE_VAST =
        "<VAST version=\"4.0\">" +
        "<Ad id=\"test-ad-001\"><InLine>" +
        "<AdSystem>Test DSP</AdSystem>" +
        "<AdTitle>Test Video Ad</AdTitle>" +
        "<Impression><![CDATA[https://track.example.com/impression]]></Impression>" +
        "<Creatives><Creative><Linear skipoffset=\"5\">" +
        "<Duration>00:00:15</Duration>" +
        "<TrackingEvents>" +
        "<Tracking event=\"start\"><![CDATA[https://track.example.com/start]]></Tracking>" +
        "<Tracking event=\"complete\"><![CDATA[https://track.example.com/complete]]></Tracking>" +
        "</TrackingEvents>" +
        "<VideoClicks>" +
        "<ClickThrough><![CDATA[https://example.com/click]]></ClickThrough>" +
        "</VideoClicks>" +
        "<MediaFiles>" +
        "<MediaFile delivery=\"progressive\" type=\"video/mp4\" width=\"640\" height=\"360\" bitrate=\"500\">" +
        "<![CDATA[https://example.com/ad.mp4]]>" +
        "</MediaFile>" +
        "</MediaFiles>" +
        "</Linear></Creative></Creatives>" +
        "</InLine></Ad></VAST>";

    private static final String INLINE_VAST_NO_SKIP =
        "<VAST version=\"4.0\">" +
        "<Ad id=\"no-skip\"><InLine>" +
        "<AdSystem>Test DSP</AdSystem>" +
        "<AdTitle>No Skip</AdTitle>" +
        "<Creatives><Creative><Linear>" +
        "<Duration>00:00:30</Duration>" +
        "<MediaFiles>" +
        "<MediaFile delivery=\"progressive\" type=\"video/mp4\" width=\"640\" height=\"360\" bitrate=\"500\">" +
        "<![CDATA[https://example.com/noskip.mp4]]>" +
        "</MediaFile>" +
        "</MediaFiles>" +
        "</Linear></Creative></Creatives>" +
        "</InLine></Ad></VAST>";

    private static final String MULTI_MEDIA_VAST =
        "<VAST version=\"4.0\">" +
        "<Ad id=\"multi\"><InLine>" +
        "<AdSystem>Test</AdSystem><AdTitle>Multi</AdTitle>" +
        "<Creatives><Creative><Linear>" +
        "<Duration>00:00:10</Duration>" +
        "<MediaFiles>" +
        "<MediaFile delivery=\"progressive\" type=\"video/mp4\" width=\"320\" height=\"180\" bitrate=\"300\">" +
        "<![CDATA[https://example.com/low.mp4]]></MediaFile>" +
        "<MediaFile delivery=\"progressive\" type=\"video/mp4\" width=\"1280\" height=\"720\" bitrate=\"2000\">" +
        "<![CDATA[https://example.com/high.mp4]]></MediaFile>" +
        "</MediaFiles>" +
        "</Linear></Creative></Creatives>" +
        "</InLine></Ad></VAST>";

    private static final String WEBM_ONLY_VAST =
        "<VAST version=\"4.0\">" +
        "<Ad id=\"webm\"><InLine>" +
        "<AdSystem>Test</AdSystem><AdTitle>WebM</AdTitle>" +
        "<Creatives><Creative><Linear>" +
        "<Duration>00:00:10</Duration>" +
        "<MediaFiles>" +
        "<MediaFile delivery=\"progressive\" type=\"video/webm\" width=\"640\" height=\"360\" bitrate=\"800\">" +
        "<![CDATA[https://example.com/ad.webm]]></MediaFile>" +
        "</MediaFiles>" +
        "</Linear></Creative></Creatives>" +
        "</InLine></Ad></VAST>";

    private static final String WRAPPER_VAST =
        "<VAST version=\"4.0\">" +
        "<Ad id=\"wrapper-001\"><Wrapper>" +
        "<AdSystem>Test DSP</AdSystem>" +
        "<VASTAdTagURI><![CDATA[https://example.com/wrapper-tag.xml]]></VASTAdTagURI>" +
        "</Wrapper></Ad></VAST>";
}
