package com.apexads.sdk.appopen;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.utils.AdLog;
import com.apexads.sdk.interstitial.InterstitialAd;
import com.apexads.sdk.interstitial.InterstitialAdListener;

import java.lang.ref.WeakReference;

@SuppressLint("StaticFieldLeak")
final class AppOpenAdManager {

    private static final long DEFAULT_EXPIRY_MS = 30 * 60 * 1_000L;

    @SuppressLint("StaticFieldLeak")
    private static volatile AppOpenAdManager sInstance;

    private Application application;
    private String placementId;
    private AppOpenAd.Listener listener;

    private final AppOpenAdFrequencyCap frequencyCap = new AppOpenAdFrequencyCap();

    private long expiryMs = DEFAULT_EXPIRY_MS;
    private long frequencyCapMs = 0L;
    private boolean enabled = true;

    private boolean destroyed = false;
    private boolean isShowingAd = false;
    private boolean isPreloading = false;

    private long adLoadedAtMs = 0L;
    private InterstitialAd currentAd;

    private WeakReference<Activity> currentActivityRef;
    private int startedActivityCount = 0;

    private boolean appInBackground = false;

    static synchronized AppOpenAdManager getInstance() {
        if (sInstance == null) sInstance = new AppOpenAdManager();
        return sInstance;
    }

    private AppOpenAdManager() {}

    synchronized void initialize(@NonNull Context context,
                                 @NonNull String placementId,
                                 @Nullable AppOpenAd.Listener listener) {
        if (!(context.getApplicationContext() instanceof Application)) {
            if (listener != null) listener.onAppOpenAdFailedToLoad(
                    new AdError.Network("AppOpenAd requires an Application context", null));
            return;
        }
        if (application != null) {
            application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks);
        }
        if (currentAd != null) {
            currentAd.destroy();
        }
        destroyed = false;
        appInBackground = false;
        startedActivityCount = 0;
        adLoadedAtMs = 0L;
        currentAd = null;
        isShowingAd = false;
        isPreloading = false;

        application = (Application) context.getApplicationContext();
        this.placementId = placementId;
        this.listener = listener;
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks);
        preload();
    }

    synchronized void setEnabled(boolean enabled) { this.enabled = enabled; }

    synchronized void setExpiryMs(long ms) { this.expiryMs = ms; }

    synchronized void setFrequencyCapMs(long ms) { this.frequencyCapMs = Math.max(0, ms); }

    synchronized boolean isAdReady() {
        return currentAd != null && currentAd.isReady() && !isAdExpired();
    }

    synchronized void destroy() {
        destroyed = true;
        if (application != null) {
            application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks);
            application = null;
        }
        if (currentAd != null) {
            currentAd.destroy();
        }
        currentAd = null;
        currentActivityRef = null;
        listener = null;
        placementId = null;
        isShowingAd = false;
        isPreloading = false;
    }

    private synchronized void preload() {
        if (destroyed || isPreloading || placementId == null) return;
        isPreloading = true;
        adLoadedAtMs = 0L;
        if (currentAd != null) {
            currentAd.destroy();
        }
        currentAd = null;

        currentAd = new InterstitialAd.Builder(placementId)
                .listener(new InterstitialAdListener() {
                    @Override
                    public void onInterstitialLoaded() {
                        synchronized (AppOpenAdManager.this) {
                            if (destroyed) return;
                            isPreloading = false;
                            adLoadedAtMs = System.currentTimeMillis();
                            AdLog.i("AppOpenAdManager: ad preloaded for placement=%s", placementId);
                            if (listener != null) listener.onAppOpenAdLoaded();
                        }
                    }

                    @Override
                    public void onInterstitialFailed(@NonNull AdError error) {
                        synchronized (AppOpenAdManager.this) {
                            if (destroyed) return;
                            isPreloading = false;
                            currentAd = null;
                            AdLog.w("AppOpenAdManager: preload failed — %s", error.getMessage());
                            if (listener != null) listener.onAppOpenAdFailedToLoad(error);
                        }
                    }

                    @Override
                    public void onInterstitialShown() {
                        synchronized (AppOpenAdManager.this) {
                            if (destroyed || application == null) return;
                            frequencyCap.record(application);
                            AdLog.d("AppOpenAdManager: ad shown");
                            if (listener != null) listener.onAppOpenAdImpression();
                        }
                    }

                    @Override
                    public void onInterstitialClosed() {
                        synchronized (AppOpenAdManager.this) {
                            if (destroyed) return;
                            isShowingAd = false;
                            currentAd = null;
                            AdLog.d("AppOpenAdManager: ad dismissed — preloading next");
                            if (listener != null) listener.onAppOpenAdDismissed();
                            preload();
                        }
                    }

                    @Override
                    public void onInterstitialClicked() {
                        synchronized (AppOpenAdManager.this) {
                            if (destroyed) return;
                            if (listener != null) listener.onAppOpenAdClicked();
                        }
                    }
                })
                .build();
        currentAd.load();
    }

    private synchronized void onAppForegrounded() {
        if (destroyed || !enabled || isShowingAd || isPreloading) return;
        if (!isAdReady()) {
            AdLog.d("AppOpenAdManager: foregrounded but no ready ad — skipping");
            return;
        }
        if (!frequencyCap.isSatisfied(application, frequencyCapMs)) {
            AdLog.d("AppOpenAdManager: frequency cap not satisfied — skipping");
            return;
        }
        showAd();
    }

    private synchronized void showAd() {
        Activity activity = currentActivityRef != null ? currentActivityRef.get() : null;
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (currentAd == null || !currentAd.isReady()) return;

        isShowingAd = true;
        AdLog.i("AppOpenAdManager: showing app open ad");
        currentAd.show(activity);
    }

    private synchronized boolean isAdExpired() {
        if (expiryMs <= 0 || adLoadedAtMs == 0L) return false;
        return (System.currentTimeMillis() - adLoadedAtMs) >= expiryMs;
    }

    private final Application.ActivityLifecycleCallbacks lifecycleCallbacks =
            new Application.ActivityLifecycleCallbacks() {

                @Override
                public void onActivityStarted(@NonNull Activity activity) {
                    synchronized (AppOpenAdManager.this) {
                        startedActivityCount++;
                    }
                }

                @Override
                public void onActivityResumed(@NonNull Activity activity) {
                    synchronized (AppOpenAdManager.this) {
                        currentActivityRef = new WeakReference<>(activity);
                        if (appInBackground) {
                            appInBackground = false;
                            onAppForegrounded();
                        }
                    }
                }

                @Override
                public void onActivityStopped(@NonNull Activity activity) {
                    synchronized (AppOpenAdManager.this) {
                        startedActivityCount = Math.max(0, startedActivityCount - 1);
                        if (startedActivityCount == 0) {
                            appInBackground = true;
                            currentActivityRef = null;
                        }
                    }
                }

                @Override public void onActivityPaused(@NonNull Activity a) {}
                @Override public void onActivityCreated(@NonNull Activity a, @Nullable Bundle b) {}
                @Override public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle b) {}
                @Override public void onActivityDestroyed(@NonNull Activity a) {}
            };
}
