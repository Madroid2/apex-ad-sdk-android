package com.apexads.sdk.internal;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.apexads.sdk.ApexAdsConfig;
import com.apexads.sdk.BuildConfig;
import com.apexads.sdk.core.audience.CohortProvider;
import com.apexads.sdk.core.consent.ConsentManager;
import com.apexads.sdk.core.device.DeviceInfoProvider;
import com.apexads.sdk.core.di.FeatureRegistry;
import com.apexads.sdk.core.di.SdkFeature;
import com.apexads.sdk.core.di.TrustDelegate;
import com.apexads.sdk.core.network.AdNetworkClient;
import com.apexads.sdk.core.network.HttpAdNetworkClient;
import com.apexads.sdk.core.network.MockAdExchange;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.network.TrackingTransport;
import com.apexads.sdk.core.network.WaterfallAdNetworkClient;
import com.apexads.sdk.core.quality.AdQualityReporter;
import com.apexads.sdk.core.tracking.PersistentTrackingQueue;
import com.apexads.sdk.core.tracking.TrackingClient;
import com.apexads.sdk.core.utils.AdLog;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ApexServices {

    private final Context appContext;
    private final ApexAdsConfig config;
    private final DeviceInfoProvider deviceInfoProvider;
    private final ConsentManager consentManager;
    private final FeatureRegistry features = new FeatureRegistry();

    private volatile AdNetworkClient networkClient;
    private volatile CohortProvider cohortProvider = CohortProvider.NONE;
    private final PersistentTrackingQueue trackingQueue;

    private ApexServices(@NonNull Context appContext,
                         @NonNull ApexAdsConfig config,
                         @NonNull AdNetworkClient networkClient,
                         @NonNull DeviceInfoProvider deviceInfoProvider,
                         @NonNull ConsentManager consentManager) {
        this.appContext = appContext;
        this.config = config;
        this.networkClient = networkClient;
        this.deviceInfoProvider = deviceInfoProvider;
        this.consentManager = consentManager;
        File queueDir = new File(appContext.getFilesDir(), "apexads");
        //noinspection ResultOfMethodCallIgnored
        queueDir.mkdirs();
        this.trackingQueue = new PersistentTrackingQueue(
                queueDir, TrackingTransport::sendGet, SdkExecutors.SINGLE, SdkExecutors.SCHEDULER);
        installQualityReporter(config);
    }

    @NonNull
    public static ApexServices create(@NonNull Application application,
                                      @NonNull ApexAdsConfig config) {
        DeviceInfoProvider deviceInfoProvider = new DeviceInfoProvider(application);
        // Advertising ID (IPC) and WebView UA are slow to resolve; warm them now so
        // the first bid request carries real identity instead of a cold cache.
        deviceInfoProvider.warmUp(SdkExecutors.IO);
        return new ApexServices(
                application.getApplicationContext(),
                config,
                new WaterfallAdNetworkClient(buildDemandSources(config)),
                deviceInfoProvider,
                new ConsentManager(application));
    }

    @NonNull
    private static List<AdNetworkClient> buildDemandSources(@NonNull ApexAdsConfig config) {
        List<AdNetworkClient> demandSources = new ArrayList<>();
        demandSources.add(new HttpAdNetworkClient(config));

        if (config.isDebugFakeFill() && BuildConfig.DEBUG) {
            demandSources.add(new MockAdExchange());
            AdLog.w("ApexAds: debugFakeFill enabled - mock demand appended (DEV ONLY)");
        } else if (config.isDebugFakeFill()) {
            AdLog.w("ApexAds: debugFakeFill ignored in non-debug builds");
        }

        return demandSources;
    }

    @NonNull
    public Context appContext() {
        return appContext;
    }

    @NonNull
    public ApexAdsConfig config() {
        return config;
    }

    @NonNull
    public AdNetworkClient networkClient() {
        return networkClient;
    }

    @NonNull
    public TrackingClient trackingClient() {
        return trackingQueue;
    }

    /**
     * Wires AdNavigationGuard block reports to the ad server's quality-report
     * endpoint so abusive creatives are quarantined fleet-wide, not just on
     * the device that blocked them.
     */
    private static void installQualityReporter(@NonNull ApexAdsConfig config) {
        String origin = serverOrigin(config.getAdServerUrl());
        if (origin == null) {
            AdLog.w("ApexAds: cannot derive server origin — quality reports disabled");
            return;
        }
        String endpoint = origin + "/sdk/v1/quality-report";
        AdQualityReporter.install((surface, reason, score, requestId, bidId, creativeId) -> {
            String json = AdQualityReporter.toJson(surface, reason, score, requestId, bidId, creativeId);
            SdkExecutors.IO.execute(() -> {
                if (!TrackingTransport.sendJson(endpoint, json)) {
                    AdLog.d("ApexAds: quality report delivery failed (best-effort)");
                }
            });
        });
    }

    @Nullable
    private static String serverOrigin(@NonNull String adServerUrl) {
        try {
            URI uri = URI.create(adServerUrl);
            if (uri.getScheme() == null || uri.getAuthority() == null) return null;
            return uri.getScheme() + "://" + uri.getAuthority();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @NonNull
    public DeviceInfoProvider deviceInfoProvider() {
        return deviceInfoProvider;
    }

    @NonNull
    public ConsentManager consentManager() {
        return consentManager;
    }

    @NonNull
    public CohortProvider cohortProvider() {
        return cohortProvider;
    }

    public void setCohortProvider(@NonNull CohortProvider cohortProvider) {
        this.cohortProvider = cohortProvider;
    }

    public void setNetworkClientForTesting(@NonNull AdNetworkClient networkClient) {
        if (!BuildConfig.DEBUG) {
            throw new IllegalStateException("Network client override is available only in debug builds.");
        }
        this.networkClient = networkClient;
    }

    public <T extends SdkFeature> void registerFeature(@NonNull Class<T> featureType,
                                                       @NonNull T feature) {
        features.register(featureType, feature);
    }

    @Nullable
    public <T extends SdkFeature> T getFeature(@NonNull Class<T> featureType) {
        return features.getOptional(featureType);
    }

    public void installFeaturesFrom(@NonNull FeatureRegistry registry) {
        features.copyFrom(registry);
    }

    public void close() {
        cohortProvider = CohortProvider.NONE;
        TrustDelegate trust = features.getOptional(TrustDelegate.class);
        if (trust != null) trust.close();
        features.clear();
        AdQualityReporter.clear();
    }
}
