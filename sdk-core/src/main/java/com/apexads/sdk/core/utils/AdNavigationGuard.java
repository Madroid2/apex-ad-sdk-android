package com.apexads.sdk.core.utils;

import android.os.Build;
import android.view.MotionEvent;
import android.webkit.WebResourceRequest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.models.AdData;

import java.util.Locale;

public final class AdNavigationGuard {

    private static final long RECENT_TOUCH_WINDOW_MS = 2_000L;
    private static final long FAST_NAVIGATION_WINDOW_MS = 1_500L;
    private static final int BLOCK_SCORE_THRESHOLD = 80;

    private final String surface;

    private long creativeLoadedAtMs;
    private long lastTouchAtMs = Long.MIN_VALUE;
    private long lastNavigationAtMs = Long.MIN_VALUE;
    private int navigationAttemptCount;
    private int jsReportCount;

    @Nullable private String requestId;
    @Nullable private String bidId;
    @Nullable private String creativeId;

    public AdNavigationGuard(@NonNull String surface) {
        this.surface = surface;
    }

    public void reset(@Nullable AdData adData) {
        long now = nowMs();
        creativeLoadedAtMs = now;
        lastTouchAtMs = Long.MIN_VALUE;
        lastNavigationAtMs = Long.MIN_VALUE;
        navigationAttemptCount = 0;
        jsReportCount = 0;

        requestId = adData != null ? adData.requestId : null;
        bidId = adData != null ? adData.bidId : null;
        creativeId = adData != null ? adData.creativeId : null;
    }

    public void recordTouchEvent(@Nullable MotionEvent event) {
        if (event == null) return;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_UP) {
            lastTouchAtMs = nowMs();
        }
    }

    @NonNull
    public Decision evaluateNavigation(@Nullable String rawUrl,
                                       @Nullable WebResourceRequest request,
                                       @NonNull String trigger) {
        boolean requestGestureKnown = false;
        boolean hasRequestGesture = false;
        if (request != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestGestureKnown = true;
            hasRequestGesture = request.hasGesture();
        }
        return evaluateNavigation(rawUrl, hasRequestGesture, requestGestureKnown, trigger);
    }

    @NonNull
    public Decision evaluateNavigation(@Nullable String rawUrl,
                                       boolean hasRequestGesture,
                                       boolean requestGestureKnown,
                                       @NonNull String trigger) {
        long now = nowMs();
        navigationAttemptCount++;

        String safeUrl = AdUrlHandler.normalizeExternalWebUrl(rawUrl);
        // A creative may click through to an app deep link (custom scheme or
        // market://). Those are legitimate destinations — validated separately,
        // still subject to the user-gesture scoring below.
        String deeplink = safeUrl == null ? AdUrlHandler.normalizeDeeplink(rawUrl) : null;
        boolean recentTouch = isRecentTouch(now);
        boolean userInitiated = hasRequestGesture || recentTouch;

        int score = 0;
        String reason = null;

        if (safeUrl == null && deeplink == null) {
            score += 100;
            reason = "unsafe_url";
        }
        if (!userInitiated) {
            score += 80;
            reason = appendReason(reason, requestGestureKnown ? "no_gesture" : "no_recent_touch");
        }
        if (creativeLoadedAtMs > 0 && now - creativeLoadedAtMs < FAST_NAVIGATION_WINDOW_MS) {
            score += 10;
        }
        if (lastNavigationAtMs != Long.MIN_VALUE
                && now - lastNavigationAtMs < FAST_NAVIGATION_WINDOW_MS) {
            score += 10;
        }
        if (navigationAttemptCount > 3) {
            score += 20;
        }

        lastNavigationAtMs = now;

        if (score >= BLOCK_SCORE_THRESHOLD) {
            if (reason == null) reason = "high_risk_navigation";
            logBlockedNavigation(rawUrl, trigger, reason, score, requestGestureKnown,
                    hasRequestGesture, recentTouch, now);
            return Decision.block(reason, score);
        }
        return deeplink != null ? Decision.allowDeeplink(deeplink, score)
                                : Decision.allow(safeUrl, score);
    }

    public void reportJsNavigationAttempt(@NonNull String type, @Nullable String rawUrl) {
        if (jsReportCount++ >= 8) return;
        AdLog.w("%s: JS navigation attempt type=%s url=%s request=%s bid=%s creative=%s",
                surface, type, redactForLog(rawUrl), nullToDash(requestId),
                nullToDash(bidId), nullToDash(creativeId));
    }

    private boolean isRecentTouch(long now) {
        return lastTouchAtMs != Long.MIN_VALUE && now - lastTouchAtMs <= RECENT_TOUCH_WINDOW_MS;
    }

    private void logBlockedNavigation(@Nullable String rawUrl,
                                      @NonNull String trigger,
                                      @NonNull String reason,
                                      int score,
                                      boolean requestGestureKnown,
                                      boolean hasRequestGesture,
                                      boolean recentTouch,
                                      long now) {
        long msSinceTouch = lastTouchAtMs == Long.MIN_VALUE ? -1L : now - lastTouchAtMs;
        long msSinceLoad = creativeLoadedAtMs <= 0 ? -1L : now - creativeLoadedAtMs;
        AdLog.w(String.format(Locale.US,
                "%s: blocked navigation reason=%s score=%d trigger=%s url=%s "
                        + "gestureKnown=%b hasGesture=%b recentTouch=%b "
                        + "msSinceTouch=%d msSinceLoad=%d attempt=%d request=%s bid=%s creative=%s",
                surface, reason, score, trigger, redactForLog(rawUrl),
                requestGestureKnown, hasRequestGesture, recentTouch,
                msSinceTouch, msSinceLoad, navigationAttemptCount,
                nullToDash(requestId), nullToDash(bidId), nullToDash(creativeId)));
    }

    @Nullable
    private static String appendReason(@Nullable String existing, @NonNull String next) {
        return existing == null ? next : existing + "+" + next;
    }

    @NonNull
    private static String redactForLog(@Nullable String rawUrl) {
        if (rawUrl == null) return "<null>";
        String trimmed = rawUrl.trim().replaceAll("\\p{Cntrl}", "?");
        return trimmed.length() > 96 ? trimmed.substring(0, 96) + "..." : trimmed;
    }

    @NonNull
    private static String nullToDash(@Nullable String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }

    private static long nowMs() {
        return System.currentTimeMillis();
    }

    void recordUserGestureForTest(long nowMs) {
        lastTouchAtMs = nowMs;
    }

    void resetClockForTest(long nowMs) {
        creativeLoadedAtMs = nowMs;
        lastNavigationAtMs = Long.MIN_VALUE;
        navigationAttemptCount = 0;
    }

    public static final class Decision {
        public final boolean allowed;
        @Nullable public final String safeUrl;
        /** Validated app deep link (custom scheme / market://); null for web URLs. */
        @Nullable public final String deeplink;
        @NonNull public final String reason;
        public final int score;

        private Decision(boolean allowed,
                         @Nullable String safeUrl,
                         @Nullable String deeplink,
                         @NonNull String reason,
                         int score) {
            this.allowed = allowed;
            this.safeUrl = safeUrl;
            this.deeplink = deeplink;
            this.reason = reason;
            this.score = score;
        }

        @NonNull
        static Decision allow(@NonNull String safeUrl, int score) {
            return new Decision(true, safeUrl, null, "allowed", score);
        }

        @NonNull
        static Decision allowDeeplink(@NonNull String deeplink, int score) {
            return new Decision(true, null, deeplink, "allowed_deeplink", score);
        }

        @NonNull
        static Decision block(@NonNull String reason, int score) {
            return new Decision(false, null, null, reason, score);
        }
    }
}
