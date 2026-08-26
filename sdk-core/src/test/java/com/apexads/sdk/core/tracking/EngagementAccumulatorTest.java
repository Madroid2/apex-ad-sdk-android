package com.apexads.sdk.core.tracking;

import static com.google.common.truth.Truth.assertThat;

import com.apexads.sdk.core.models.EngagementMeasurement;

import org.junit.Test;

public class EngagementAccumulatorTest {

    @Test
    public void sample_countsOnlyViewableTimeAndFiresEachMilestoneOnce() {
        EngagementAccumulator accumulator = new EngagementAccumulator(measurement(), "BANNER");

        assertThat(accumulator.sample(1_000, true)).isEmpty();
        assertThat(accumulator.sample(1_250, true)).isEmpty();
        accumulator.pause();
        assertThat(accumulator.sample(50_000, false)).isEmpty();
        assertThat(accumulator.sample(50_750, true)).isEmpty();
        assertThat(accumulator.sample(51_000, true)).isEmpty();
        assertThat(accumulator.sample(51_500, true)).containsExactly("https://track.example/1s");
        assertThat(accumulator.sample(52_000, true)).isEmpty();
        assertThat(accumulator.getVisibleDurationMs()).isEqualTo(1_500L);
    }

    @Test
    public void signal_isDeduplicated() {
        EngagementAccumulator accumulator = new EngagementAccumulator(measurement(), "BANNER");
        assertThat(accumulator.signal("CUSTOM")).containsExactly("https://track.example/custom");
        assertThat(accumulator.signal("CUSTOM")).isEmpty();
    }

    private EngagementMeasurement measurement() {
        return EngagementMeasurement.fromJson("{"
                + "\"version\":1,\"min_visible_ratio\":0.5,\"sample_interval_ms\":250,"
                + "\"events\":["
                + "{\"surface\":\"BANNER\",\"signal\":\"VIEWABLE_DWELL\","
                + "\"value_ms\":1000,\"threshold_ms\":1000,\"url\":\"https://track.example/1s\"},"
                + "{\"surface\":\"BANNER\",\"signal\":\"CUSTOM\","
                + "\"value_ms\":0,\"url\":\"https://track.example/custom\"}]} ");
    }
}
