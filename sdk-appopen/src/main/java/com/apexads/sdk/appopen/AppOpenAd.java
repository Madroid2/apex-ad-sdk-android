package com.apexads.sdk.appopen;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.error.AdError;

/**
 * App Open Ad — shows a fullscreen interstitial automatically whenever the user
 * returns the app from background to foreground.
 *
 * <b>Only AdMob offers a first-class App Open Ad format in their SDK.</b>
 * ApexAds provides this capability natively, with zero dependency on Google's SDK,
 * backed by the same OpenRTB pipeline as every other ad format.
 *
 * <h3>Setup (Application.onCreate)</h3>
 * <pre>{@code
 * AppOpenAd.initialize(this, "placement-appopen", new AppOpenAd.Listener() {
 *     public void onAppOpenAdLoaded()   { Log.d(TAG, "App open ad preloaded"); }
 *     public void onAppOpenAdDismissed(){ Log.d(TAG, "App open ad dismissed"); }
 *     public void onAppOpenAdFailedToLoad(AdError e) { Log.w(TAG, e.getMessage()); }
 * });
 * // Optional tuning:
 * AppOpenAd.setFrequencyCapHours(1);   // at most once per hour
 * AppOpenAd.setAdExpiryMinutes(30);    // discard cached ad after 30 min
 * }</pre>
 *
 * <h3>Teardown</h3>
 * <pre>{@code
 * AppOpenAd.destroy(); // call from Application.onTerminate or when ads are no longer needed
 * }</pre>
 */
public final class AppOpenAd {

    private AppOpenAd() {}

    // ── Listener ──────────────────────────────────────────────────────────────

    public interface Listener {
        /** Called on main thread when an ad has been fetched and is ready to show. */
        void onAppOpenAdLoaded();

        /** Called on main thread when the fetch fails. The ad will not be shown. */
        void onAppOpenAdFailedToLoad(@NonNull AdError error);

        /** Called on main thread when the ad impression fires. */
        default void onAppOpenAdImpression() {}

        /** Called on main thread when the user dismisses the ad. */
        default void onAppOpenAdDismissed() {}

        /** Called on main thread when the user taps the ad. */
        default void onAppOpenAdClicked() {}
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Initialises App Open Ads. Call once from {@code Application.onCreate()}.
     * Registers an {@link android.app.Application.ActivityLifecycleCallbacks} internally —
     * no additional wiring required in individual Activities.
     *
     * @param context     Application context
     * @param placementId Your App Open placement ID from the ApexAds dashboard
     * @param listener    Callbacks (may be null)
     */
    public static void initialize(@NonNull Context context,
                                  @NonNull String placementId,
                                  @Nullable Listener listener) {
        AppOpenAdManager.getInstance().initialize(context, placementId, listener);
    }

    /** Returns true when an ad has been fetched and has not yet expired. */
    public static boolean isAdReady() {
        return AppOpenAdManager.getInstance().isAdReady();
    }

    /** Enables or disables App Open Ads without destroying the preloaded ad. Default: enabled. */
    public static void setEnabled(boolean enabled) {
        AppOpenAdManager.getInstance().setEnabled(enabled);
    }

    /**
     * Frequency cap — minimum time between successive App Open ad shows.
     * Pass 0 to disable (show every time the app foregrounds, if an ad is ready).
     * Default: 0 (uncapped).
     */
    public static void setFrequencyCapHours(int hours) {
        AppOpenAdManager.getInstance().setFrequencyCapMs(Math.max(0, hours) * 3_600_000L);
    }

    /**
     * Maximum age of a preloaded ad. If the cached ad is older than this when the app
     * foregrounds, it is discarded and a new request is fired.
     * Pass 0 to disable expiry. Default: 30 minutes.
     */
    public static void setAdExpiryMinutes(int minutes) {
        AppOpenAdManager.getInstance().setExpiryMs(minutes * 60_000L);
    }

    /** Unregisters lifecycle callbacks and releases all held resources. */
    public static void destroy() {
        AppOpenAdManager.getInstance().destroy();
    }
}
