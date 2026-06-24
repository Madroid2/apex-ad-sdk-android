package com.apexads.sdk.internal;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.apexads.sdk.ApexAdsConfig;
import com.apexads.sdk.BuildConfig;
import com.apexads.sdk.core.audience.CohortProvider;
import com.apexads.sdk.core.audience.DeclarativeCohortProvider;
import com.apexads.sdk.core.consent.ConsentManager;
import com.apexads.sdk.core.crashreporter.CrashReporter;
import com.apexads.sdk.core.device.DeviceInfoProvider;
import com.apexads.sdk.core.network.AdNetworkClient;
import com.apexads.sdk.core.tracking.TrackingClient;
import com.apexads.sdk.core.utils.AdLog;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ApexSdkRuntime {

    private static volatile ApexServices services;

    private ApexSdkRuntime() {}

    public static synchronized void init(@NonNull Application application,
                                         @NonNull ApexAdsConfig cfg) {
        if (services != null) {
            throw new IllegalStateException("ApexAds SDK is already initialized.");
        }

        AdLog.enable(cfg.isDebugLogging() || BuildConfig.DEBUG);
        CrashReporter.init(cfg.getSentryDsn());

        ApexServices created = ApexServices.create(application, cfg);
        ApexFeatureAccess.attach(created);
        services = created;

        AdLog.i("ApexAds SDK v%s initialized [appToken=%s...]",
                BuildConfig.SDK_VERSION,
                cfg.getAppToken().substring(0, Math.min(8, cfg.getAppToken().length())));
    }

    public static boolean isInitialized() {
        return services != null;
    }

    @NonNull
    public static Context getContext() {
        return getServices().appContext();
    }

    @NonNull
    public static ApexAdsConfig getConfig() {
        return getServices().config();
    }

    @NonNull
    public static ConsentManager getConsentManager() {
        return getServices().consentManager();
    }

    @NonNull
    public static AdNetworkClient getNetworkClient() {
        return getServices().networkClient();
    }

    @NonNull
    public static TrackingClient getTrackingClient() {
        return getServices().trackingClient();
    }

    @NonNull
    public static DeviceInfoProvider getDeviceInfoProvider() {
        return getServices().deviceInfoProvider();
    }

    public static void setAudienceCohortRules(String rulesJson) {
        ApexServices current = getServices();
        if (rulesJson == null || rulesJson.trim().isEmpty()) {
            current.setCohortProvider(CohortProvider.NONE);
            AdLog.i("ApexAds: audience cohorts cleared");
            return;
        }
        DeclarativeCohortProvider provider = DeclarativeCohortProvider.fromJson(rulesJson);
        current.setCohortProvider(provider);
        AdLog.i("ApexAds: %d audience cohort rule(s) active", provider.size());
    }

    @NonNull
    public static CohortProvider getCohortProvider() {
        return getServices().cohortProvider();
    }

    public static synchronized void reset() {
        ApexServices current = services;
        services = null;
        ApexFeatureAccess.detach();
        if (current != null) {
            current.close();
        }
    }

    public static synchronized void setNetworkClientForTesting(
            @NonNull AdNetworkClient networkClient) {
        getServices().setNetworkClientForTesting(networkClient);
    }

    @NonNull
    private static ApexServices getServices() {
        ApexServices current = services;
        if (current == null) {
            throw new IllegalStateException(
                    "ApexAds SDK is not initialized. Call ApexAds.init() in Application.onCreate().");
        }
        return current;
    }
}
