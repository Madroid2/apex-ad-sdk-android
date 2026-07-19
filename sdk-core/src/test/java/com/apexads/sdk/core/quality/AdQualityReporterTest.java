package com.apexads.sdk.core.quality;

import static com.google.common.truth.Truth.assertThat;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public final class AdQualityReporterTest {

    private final List<String> reports = new ArrayList<>();

    @After
    public void tearDown() {
        AdQualityReporter.clear();
    }

    @Test
    public void reportReachesInstalledSink() {
        AdQualityReporter.install((surface, reason, score, requestId, bidId, creativeId) ->
                reports.add(surface + "/" + reason + "/" + score + "/" + creativeId));

        AdQualityReporter.reportNavigationBlocked("BannerAdView", "no_gesture", 90, "r1", "b1", "cr1");

        assertThat(reports).containsExactly("BannerAdView/no_gesture/90/cr1");
    }

    @Test
    public void noSinkIsSafe() {
        AdQualityReporter.clear();
        AdQualityReporter.reportNavigationBlocked("BannerAdView", "no_gesture", 90, null, null, null);
        assertThat(reports).isEmpty();
    }

    @Test
    public void throwingSinkIsContained() {
        AdQualityReporter.install((surface, reason, score, requestId, bidId, creativeId) -> {
            throw new IllegalStateException("boom");
        });
        AdQualityReporter.reportNavigationBlocked("BannerAdView", "no_gesture", 90, null, null, null);
        // no exception propagated
    }

    @Test
    public void toJsonMatchesServerContract() throws Exception {
        String json = AdQualityReporter.toJson("mraid", "unsafe_url", 100, "req-1", "bid-1", "cr-1");

        JSONObject o = new JSONObject(json);
        assertThat(o.getString("creative_id")).isEqualTo("cr-1");
        assertThat(o.getString("bid_id")).isEqualTo("bid-1");
        assertThat(o.getString("request_id")).isEqualTo("req-1");
        assertThat(o.getString("reason")).isEqualTo("unsafe_url");
        assertThat(o.getInt("score")).isEqualTo(100);
        assertThat(o.getString("surface")).isEqualTo("mraid");
    }

    @Test
    public void toJsonEscapesSpecialCharacters() throws Exception {
        String json = AdQualityReporter.toJson("s", "reason\"with\\quotes\n", 1, null, null, "cr");

        JSONObject o = new JSONObject(json);
        assertThat(o.getString("reason")).isEqualTo("reason\"with\\quotes\n");
        assertThat(o.getString("request_id")).isEmpty();
    }
}
