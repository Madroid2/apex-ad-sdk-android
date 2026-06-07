package com.apexads.sdk.core.crashreporter;

import android.os.Build;

import androidx.annotation.NonNull;

import com.apexads.sdk.BuildConfig;

import java.util.UUID;

/**
 * Crash data model serialized to Sentry envelope format (JSON, no 3p deps).
 */
final class CrashEvent {

    final String eventId;
    final String timestamp;
    final String platform = "java";
    final String level = "fatal";
    final String release;
    final Throwable throwable;
    final String deviceModel;
    final String osVersion;

    CrashEvent(@NonNull Throwable throwable) {
        this.eventId = UUID.randomUUID().toString().replace("-", "");
        this.timestamp = iso8601Now();
        this.throwable = throwable;
        this.release = BuildConfig.SDK_VERSION;
        this.deviceModel = Build.MODEL;
        this.osVersion = String.valueOf(Build.VERSION.SDK_INT);
    }

    /** Serializes the event as a Sentry envelope (header\n{}\n{event}). */
    @NonNull
    String toEnvelope(@NonNull String publicKey) {
        String envelopeHeader = "{\"dsn\":\"" + publicKey + "\",\"sdk\":{\"name\":\"apex-ad-sdk\",\"version\":\"" + release + "\"}}";

        String stacktrace = buildStacktrace();
        String eventBody = "{"
                + "\"event_id\":\"" + eventId + "\","
                + "\"timestamp\":\"" + timestamp + "\","
                + "\"platform\":\"" + platform + "\","
                + "\"level\":\"" + level + "\","
                + "\"release\":\"" + release + "\","
                + "\"contexts\":{\"device\":{\"model\":\"" + deviceModel + "\"},"
                + "\"os\":{\"name\":\"Android\",\"version\":\"" + osVersion + "\"}},"
                + "\"exception\":{\"values\":[{"
                + "\"type\":\"" + escapeJson(throwable.getClass().getName()) + "\","
                + "\"value\":\"" + escapeJson(throwable.getMessage() != null ? throwable.getMessage() : "") + "\","
                + "\"stacktrace\":{\"frames\":" + stacktrace + "}"
                + "}]}"
                + "}";

        String itemHeader = "{\"type\":\"event\",\"length\":" + eventBody.length() + "}";

        return envelopeHeader + "\n" + itemHeader + "\n" + eventBody;
    }

    private String buildStacktrace() {
        StackTraceElement[] frames = throwable.getStackTrace();
        StringBuilder sb = new StringBuilder("[");
        for (int i = frames.length - 1; i >= 0; i--) {
            StackTraceElement f = frames[i];
            if (i < frames.length - 1) sb.append(",");
            sb.append("{\"filename\":\"").append(escapeJson(f.getFileName() != null ? f.getFileName() : "")).append("\"")
              .append(",\"module\":\"").append(escapeJson(f.getClassName())).append("\"")
              .append(",\"function\":\"").append(escapeJson(f.getMethodName())).append("\"")
              .append(",\"lineno\":").append(f.getLineNumber())
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String iso8601Now() {
        // Use simple formatting without java.time to keep minSdk compatibility
        java.util.Date d = new java.util.Date();
        java.util.TimeZone tz = java.util.TimeZone.getTimeZone("UTC");
        java.util.Calendar cal = java.util.Calendar.getInstance(tz);
        cal.setTime(d);
        return String.format(java.util.Locale.US,
                "%04d-%02d-%02dT%02d:%02d:%02d.000Z",
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH),
                cal.get(java.util.Calendar.HOUR_OF_DAY),
                cal.get(java.util.Calendar.MINUTE),
                cal.get(java.util.Calendar.SECOND));
    }
}
