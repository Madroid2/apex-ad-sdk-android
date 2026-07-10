package com.apexads.sdk.appopen;

import android.content.Context;
import android.content.SharedPreferences;

final class AppOpenAdFrequencyCap {

    private static final String PREFS_NAME  = "apex_app_open_prefs";
    private static final String KEY_LAST_MS = "last_shown_ms";

    boolean isSatisfied(Context context, long capMs) {
        if (capMs <= 0) return true;
        long last = prefs(context).getLong(KEY_LAST_MS, 0L);
        return last == 0L || (System.currentTimeMillis() - last) >= capMs;
    }

    void record(Context context) {
        prefs(context).edit().putLong(KEY_LAST_MS, System.currentTimeMillis()).apply();
    }

    void reset(Context context) {
        prefs(context).edit().remove(KEY_LAST_MS).apply();
    }

    private SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
