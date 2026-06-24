package com.apexads.sdk;

import android.app.Application;

import androidx.annotation.NonNull;

import com.apexads.sdk.internal.ApexSdkRuntime;

public final class ApexAds {

    private ApexAds() {}

    /**
     * Initializes ApexAds once from {@link Application#onCreate()}.
     */
    public static void init(@NonNull Application application,
                            @NonNull ApexAdsConfig cfg) {
        ApexSdkRuntime.init(application, cfg);
    }
}
