package com.apexads.sdk.core.tracking;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.models.EngagementMeasurement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Monotonic, UI-independent accumulation core used by {@link EngagementTracker}. */
final class EngagementAccumulator {

    private final List<EngagementMeasurement.Event> events;
    private final Set<String> firedUrls = new HashSet<>();
    private final long maxSampleGapMs;
    private long visibleDurationMs;
    private long lastSampleMs = -1L;
    private boolean lastViewable;

    EngagementAccumulator(@NonNull EngagementMeasurement measurement, @NonNull String surface) {
        events = new ArrayList<>(measurement.eventsForSurface(surface));
        events.sort(Comparator.comparingLong(event -> event.thresholdMs));
        maxSampleGapMs = measurement.sampleIntervalMs * 4L;
    }

    @NonNull
    List<String> sample(long nowMs, boolean viewable) {
        List<String> ready = new ArrayList<>();
        if (lastSampleMs >= 0L && nowMs >= lastSampleMs && viewable && lastViewable) {
            visibleDurationMs += Math.min(nowMs - lastSampleMs, maxSampleGapMs);
        }
        lastSampleMs = nowMs;
        lastViewable = viewable;
        for (EngagementMeasurement.Event event : events) {
            if (event.thresholdMs >= 0L && visibleDurationMs >= event.thresholdMs && firedUrls.add(event.url)) {
                ready.add(event.url);
            }
        }
        return ready;
    }

    @NonNull
    List<String> signal(@NonNull String signal) {
        List<String> ready = new ArrayList<>();
        for (EngagementMeasurement.Event event : events) {
            if (event.thresholdMs < 0L && signal.equals(event.signal) && firedUrls.add(event.url)) {
                ready.add(event.url);
            }
        }
        return ready;
    }

    void pause() {
        lastSampleMs = -1L;
        lastViewable = false;
    }

    long getVisibleDurationMs() {
        return visibleDurationMs;
    }
}
