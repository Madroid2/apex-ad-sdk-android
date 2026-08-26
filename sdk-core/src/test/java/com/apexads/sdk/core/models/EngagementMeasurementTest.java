package com.apexads.sdk.core.models;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class EngagementMeasurementTest {

    @Test
    public void fromJson_parsesSignedMilestonesAndFiltersBySurface() {
        EngagementMeasurement measurement = EngagementMeasurement.fromJson("{"
                + "\"version\":1,\"min_visible_ratio\":0.5,\"sample_interval_ms\":250,"
                + "\"events\":["
                + "{\"surface\":\"BANNER\",\"signal\":\"VIEWABLE_DWELL\","
                + "\"value_ms\":1000,\"threshold_ms\":1000,\"url\":\"https://track.example/one\"},"
                + "{\"surface\":\"VIDEO\",\"signal\":\"VIDEO_SKIP_AVAILABLE\","
                + "\"value_ms\":0,\"url\":\"https://track.example/skip\"}]} ");

        assertThat(measurement).isNotNull();
        assertThat(measurement.minVisibleRatio).isEqualTo(0.5f);
        assertThat(measurement.eventsForSurface("BANNER")).hasSize(1);
        assertThat(measurement.eventsForSurface("VIDEO").get(0).signal)
                .isEqualTo("VIDEO_SKIP_AVAILABLE");
    }

    @Test
    public void fromJson_rejectsUnknownVersionAndUnsafeUrl() {
        assertThat(EngagementMeasurement.fromJson("{\"version\":2,\"events\":[]}"))
                .isNull();
        assertThat(EngagementMeasurement.fromJson("{"
                + "\"version\":1,\"events\":[{\"surface\":\"BANNER\","
                + "\"signal\":\"VIEWABLE_DWELL\",\"value_ms\":1000,"
                + "\"threshold_ms\":1000,\"url\":\"javascript:alert(1)\"}]}"))
                .isNull();
    }
}
