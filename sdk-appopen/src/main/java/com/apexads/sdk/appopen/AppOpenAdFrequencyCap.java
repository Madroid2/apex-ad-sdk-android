package com.apexads.sdk.appopen;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists the timestamp of the last App Open ad shown and compares it
 * against the publisher-configured frequency cap window.
 */
final class AppOpenAdFrequencyCap {

    private static final String PREFS_NAME  = "apex_app_open_prefs";
    private static final String KEY_LAST_MS = "last_shown_ms";

    /** Returns true when the cap allows showing (or cap is disabled with capMs ≤ 0). */
    boolean isSatisfied(Context context, long capMs) {
        if (capMs <= 0) return true;
        long last = prefs(context).getLong(KEY_LAST_MS, 0L);
        return last == 0L || (System.currentTimeMillis() - last) >= capMs;
    }

    /** Records the current time as the last-shown timestamp. */
    void record(Context context) {
        prefs(context).edit().putLong(KEY_LAST_MS, System.currentTimeMillis()).apply();
    }

    /** Clears the recorded timestamp (e.g. for testing). */
    void reset(Context context) {
        prefs(context).edit().remove(KEY_LAST_MS).apply();
    }

    private SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
