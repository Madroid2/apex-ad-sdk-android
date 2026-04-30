package com.apexads.sdk.core.utils;

import android.util.Log;

/**
 * Lightweight SDK logger backed by {@link android.util.Log}.
 *
 * Debug and info levels are gated by {@link #enable(boolean)}; warn and error
 * always fire so operators can diagnose issues without enabling verbose logging.
 */
public final class AdLog {

    private static final String TAG = "ApexAds";
    private static volatile boolean sEnabled = false;

    private AdLog() {}

    /** Enable or disable debug/info logging. Call from ApexAds.init(). */
    public static void enable(boolean on) {
        sEnabled = on;
    }

    // ── Debug ─────────────────────────────────────────────────────────────────

    public static void d(String msg) {
        if (sEnabled) Log.d(TAG, msg);
    }

    public static void d(String format, Object... args) {
        if (sEnabled) Log.d(TAG, String.format(format, args));
    }

    // ── Info ──────────────────────────────────────────────────────────────────

    public static void i(String format, Object... args) {
        if (sEnabled) Log.i(TAG, String.format(format, args));
    }

    // ── Warn ──────────────────────────────────────────────────────────────────

    public static void w(String msg) {
        Log.w(TAG, msg);
    }

    public static void w(String format, Object... args) {
        Log.w(TAG, String.format(format, args));
    }

    public static void w(Throwable t, String format, Object... args) {
        Log.w(TAG, String.format(format, args), t);
    }

    // ── Error ─────────────────────────────────────────────────────────────────

    public static void e(String msg) {
        Log.e(TAG, msg);
    }

    public static void e(Throwable t, String msg) {
        Log.e(TAG, msg, t);
    }

    public static void e(Throwable t, String format, Object... args) {
        Log.e(TAG, String.format(format, args), t);
    }
}
