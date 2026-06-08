package com.apexads.sdk;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.consent.ConsentManager;
import com.apexads.sdk.core.crashreporter.CrashReporter;
import com.apexads.sdk.core.device.DeviceInfoProvider;
import com.apexads.sdk.core.di.ServiceLocator;
import com.apexads.sdk.core.network.AdNetworkClient;
import com.apexads.sdk.core.network.HttpAdNetworkClient;
import com.apexads.sdk.core.network.MockAdExchange;
import com.apexads.sdk.core.network.WaterfallAdNetworkClient;
import com.apexads.sdk.core.utils.AdLog;

import java.util.ArrayList;
import java.util.List;

public final class ApexAds {

    private static volatile boolean initialized = false;
    private static Context appContext;
    private static ApexAdsConfig config;

    private ApexAds() {}

    public static synchronized void init(@NonNull Application application,
                                         @NonNull ApexAdsConfig cfg) {
        if (initialized) {
            throw new IllegalStateException("ApexAds SDK is already initialized.");
        }
        appContext = application.getApplicationContext();
        config = cfg;

        AdLog.enable(cfg.isDebugLogging() || BuildConfig.DEBUG);
        CrashReporter.init(cfg.getSentryDsn());

        bootstrapServiceLocator(application);
        initialized = true;

        AdLog.i("ApexAds SDK v%s initialized [appToken=%s…]",
                BuildConfig.SDK_VERSION,
                cfg.getAppToken().substring(0, Math.min(8, cfg.getAppToken().length())));
    }

    public static boolean isInitialized() {
        return initialized;
    }

    @NonNull
    public static Context getContext() {
        checkInitialized();
        return appContext;
    }

    @NonNull
    public static ApexAdsConfig getConfig() {
        checkInitialized();
        return config;
    }

    @NonNull
    public static ConsentManager getConsentManager() {
        checkInitialized();
        return ServiceLocator.get(ConsentManager.class);
    }

    @NonNull
    public static AdNetworkClient getNetworkClient() {
        checkInitialized();
        return ServiceLocator.get(AdNetworkClient.class);
    }

    @NonNull
    public static DeviceInfoProvider getDeviceInfoProvider() {
        checkInitialized();
        return ServiceLocator.get(DeviceInfoProvider.class);
    }

    public static synchronized void reset() {
        if (initialized) {
            ServiceLocator.reset();
            initialized = false;
        }
    }

    private static void bootstrapServiceLocator(Application application) {

        List<AdNetworkClient> demandSources = new ArrayList<>();
        demandSources.add(new HttpAdNetworkClient(config));

        if (config.isDebugFakeFill()) {
            demandSources.add(new MockAdExchange());
            AdLog.w("ApexAds: debugFakeFill enabled — mock demand appended (DEV ONLY)");
        }

        AdNetworkClient networkClient = new WaterfallAdNetworkClient(demandSources);

        DeviceInfoProvider deviceInfoProvider = new DeviceInfoProvider(application);
        ConsentManager consentManager = new ConsentManager(application);

        ServiceLocator.register(AdNetworkClient.class, networkClient);
        ServiceLocator.register(DeviceInfoProvider.class, deviceInfoProvider);
        ServiceLocator.register(ConsentManager.class, consentManager);
    }

    private static void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                    "ApexAds SDK is not initialized. Call ApexAds.init() in Application.onCreate().");
        }
    }
}
