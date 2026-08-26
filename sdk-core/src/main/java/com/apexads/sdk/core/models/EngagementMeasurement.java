package com.apexads.sdk.core.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Privacy-minimized, server-signed attention measurement instructions. */
public final class EngagementMeasurement {

    public static final int SUPPORTED_VERSION = 1;

    public final int version;
    public final float minVisibleRatio;
    public final int sampleIntervalMs;
    public final List<Event> events;

    private EngagementMeasurement(int version, float minVisibleRatio,
                                  int sampleIntervalMs, List<Event> events) {
        this.version = version;
        this.minVisibleRatio = minVisibleRatio;
        this.sampleIntervalMs = sampleIntervalMs;
        this.events = Collections.unmodifiableList(events);
    }

    @Nullable
    public static EngagementMeasurement fromJson(@Nullable String json) {
        if (json == null || json.trim().isEmpty()) return null;
        try {
            JSONObject root = new JSONObject(json);
            int version = root.optInt("version", 0);
            if (version != SUPPORTED_VERSION) return null;
            float ratio = (float) root.optDouble("min_visible_ratio", 0.5);
            int interval = root.optInt("sample_interval_ms", 250);
            if (ratio < 0.5f || ratio > 1f || interval < 100 || interval > 1_000) return null;

            JSONArray rawEvents = root.optJSONArray("events");
            if (rawEvents == null || rawEvents.length() == 0 || rawEvents.length() > 32) return null;
            List<Event> events = new ArrayList<>();
            for (int i = 0; i < rawEvents.length(); i++) {
                JSONObject raw = rawEvents.optJSONObject(i);
                if (raw == null) continue;
                String surface = raw.optString("surface", "").trim();
                String signal = raw.optString("signal", "").trim();
                String url = raw.optString("url", "").trim();
                long valueMs = raw.optLong("value_ms", 0L);
                long thresholdMs = raw.has("threshold_ms")
                        ? raw.optLong("threshold_ms", -1L) : -1L;
                if (surface.isEmpty() || signal.isEmpty() || valueMs < 0 ||
                        thresholdMs > 120_000L || !isTrackingUrl(url)) {
                    continue;
                }
                events.add(new Event(surface, signal, valueMs, thresholdMs, url));
            }
            return events.isEmpty() ? null : new EngagementMeasurement(version, ratio, interval, events);
        } catch (Exception ignored) {
            return null;
        }
    }

    @NonNull
    public List<Event> eventsForSurface(@NonNull String surface) {
        List<Event> result = new ArrayList<>();
        for (Event event : events) {
            if (surface.equals(event.surface)) result.add(event);
        }
        return result;
    }

    private static boolean isTrackingUrl(String url) {
        return url.length() <= 4096 && (url.startsWith("https://") || url.startsWith("http://"));
    }

    public static final class Event {
        public final String surface;
        public final String signal;
        public final long valueMs;
        public final long thresholdMs;
        public final String url;

        Event(String surface, String signal, long valueMs, long thresholdMs, String url) {
            this.surface = surface;
            this.signal = signal;
            this.valueMs = valueMs;
            this.thresholdMs = thresholdMs;
            this.url = url;
        }
    }
}
