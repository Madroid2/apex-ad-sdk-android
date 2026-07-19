package com.apexads.sdk.core.quality;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.utils.AdLog;

/**
 * Fleet feedback channel for creative abuse.
 *
 * When {@code AdNavigationGuard} blocks a forced redirect on one device, the
 * block is reported to the ad server's quality-report endpoint, where repeat
 * offenders are quarantined fleet-wide. Without this loop a bad creative gets
 * blocked device-by-device forever; with it, a handful of blocks retires the
 * creative for everyone.
 *
 * The sink is installed by the SDK runtime at init. Reporting is best-effort
 * and must never interfere with rendering: failures are swallowed.
 */
public final class AdQualityReporter {

    /** Receives navigation-block reports. Implementations must not throw. */
    public interface Sink {
        void onNavigationBlocked(@NonNull String surface,
                                 @NonNull String reason,
                                 int score,
                                 @Nullable String requestId,
                                 @Nullable String bidId,
                                 @Nullable String creativeId);
    }

    private static volatile Sink sink;

    private AdQualityReporter() {}

    public static void install(@Nullable Sink s) {
        sink = s;
    }

    public static void clear() {
        sink = null;
    }

    public static void reportNavigationBlocked(@NonNull String surface,
                                               @NonNull String reason,
                                               int score,
                                               @Nullable String requestId,
                                               @Nullable String bidId,
                                               @Nullable String creativeId) {
        Sink s = sink;
        if (s == null) return;
        try {
            s.onNavigationBlocked(surface, reason, score, requestId, bidId, creativeId);
        } catch (RuntimeException e) {
            AdLog.w(e, "AdQualityReporter: sink failed");
        }
    }

    /** Builds the /sdk/v1/quality-report JSON payload. */
    @NonNull
    public static String toJson(@NonNull String surface,
                                @NonNull String reason,
                                int score,
                                @Nullable String requestId,
                                @Nullable String bidId,
                                @Nullable String creativeId) {
        StringBuilder sb = new StringBuilder(160);
        sb.append('{');
        appendField(sb, "creative_id", creativeId == null ? "" : creativeId);
        sb.append(',');
        appendField(sb, "bid_id", bidId == null ? "" : bidId);
        sb.append(',');
        appendField(sb, "request_id", requestId == null ? "" : requestId);
        sb.append(',');
        appendField(sb, "reason", reason);
        sb.append(",\"score\":").append(score).append(',');
        appendField(sb, "surface", surface);
        sb.append('}');
        return sb.toString();
    }

    private static void appendField(StringBuilder sb, String name, String value) {
        sb.append('"').append(name).append("\":\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c < 0x20) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
    }
}
