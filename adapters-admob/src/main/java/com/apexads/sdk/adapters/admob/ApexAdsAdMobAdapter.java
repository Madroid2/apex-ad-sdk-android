package com.apexads.sdk.adapters.admob;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.VersionInfo;
import com.google.android.gms.ads.mediation.Adapter;
import com.google.android.gms.ads.mediation.InitializationCompleteCallback;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;
import com.google.android.gms.ads.mediation.MediationConfiguration;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;

import java.util.List;

/**
 * AdMob mediation adapter entry point for ApexAds.
 *
 * Register in your AdMob dashboard custom event or mediation group:
 *   Class name: com.apexads.sdk.adapters.admob.ApexAdsAdMobAdapter
 *
 * Server-side parameter JSON: {"placementId":"your-id","appToken":"your-token"}
 */
public final class ApexAdsAdMobAdapter extends Adapter {

    private static final int ADAPTER_MAJOR = 1;
    private static final int ADAPTER_MINOR = 0;
    private static final int ADAPTER_PATCH = 0;

    @NonNull
    @Override
    public VersionInfo getVersionInfo() {
        return new VersionInfo(ADAPTER_MAJOR, ADAPTER_MINOR, ADAPTER_PATCH);
    }

    @NonNull
    @Override
    public VersionInfo getSDKVersionInfo() {
        // Reflect the sdk-core BuildConfig version at runtime
        return new VersionInfo(1, 0, 0);
    }

    @Override
    public void initialize(@NonNull android.content.Context context,
                           @NonNull InitializationCompleteCallback callback,
                           @NonNull List<MediationConfiguration> configs) {
        // ApexAds is initialized by the publisher in Application.onCreate() — nothing to do here.
        callback.onInitializationSucceeded();
    }

    @Override
    public void loadBannerAd(@NonNull MediationBannerAdConfiguration config,
                             @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> cb) {
        ApexAdsBannerAdapter.load(config, cb);
    }

    @Override
    public void loadInterstitialAd(@NonNull MediationInterstitialAdConfiguration config,
                                   @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> cb) {
        ApexAdsInterstitialAdapter.load(config, cb);
    }

    @Override
    public void loadRewardedAd(@NonNull MediationRewardedAdConfiguration config,
                               @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> cb) {
        ApexAdsRewardedAdapter.load(config, cb);
    }
}
