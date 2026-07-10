package com.apexads.sdk.appopen;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.error.AdError;

public final class AppOpenAd {

    private AppOpenAd() {}

    public interface Listener {

        void onAppOpenAdLoaded();

        void onAppOpenAdFailedToLoad(@NonNull AdError error);

        default void onAppOpenAdImpression() {}

        default void onAppOpenAdDismissed() {}

        default void onAppOpenAdClicked() {}
    }

    public static void initialize(@NonNull Context context,
                                  @NonNull String placementId,
                                  @Nullable Listener listener) {
        AppOpenAdManager.getInstance().initialize(context, placementId, listener);
    }

    public static boolean isAdReady() {
        return AppOpenAdManager.getInstance().isAdReady();
    }

    public static void setEnabled(boolean enabled) {
        AppOpenAdManager.getInstance().setEnabled(enabled);
    }

    public static void setFrequencyCapHours(int hours) {
        AppOpenAdManager.getInstance().setFrequencyCapMs(Math.max(0, hours) * 3_600_000L);
    }

    public static void setAdExpiryMinutes(int minutes) {
        AppOpenAdManager.getInstance().setExpiryMs(minutes * 60_000L);
    }

    public static void destroy() {
        AppOpenAdManager.getInstance().destroy();
    }
}
