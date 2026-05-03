package com.apexads.sdk.adapters.admob;

import android.content.Context;

import androidx.annotation.NonNull;

import com.apexads.sdk.interstitial.InterstitialAd;
import com.apexads.sdk.interstitial.InterstitialAdListener;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.utils.AdLog;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAd;
import com.google.android.gms.ads.mediation.MediationInterstitialAdCallback;
import com.google.android.gms.ads.mediation.MediationInterstitialAdConfiguration;

/**
 * AdMob mediation adapter — Interstitial.
 */
public final class ApexAdsInterstitialAdapter implements MediationInterstitialAd {

    private InterstitialAd interstitialAd;
    private MediationInterstitialAdCallback adCallback;
    private Context context;

    public ApexAdsInterstitialAdapter() {}

    public static void load(
            @NonNull MediationInterstitialAdConfiguration config,
            @NonNull MediationAdLoadCallback<MediationInterstitialAd, MediationInterstitialAdCallback> loadCallback) {

        String placementId = ApexAdsAdMobUtils.getPlacementId(config);
        if (placementId == null) {
            loadCallback.onFailure(new com.google.android.gms.ads.AdError(
                    0, "Missing placementId in server parameters", "ApexAds"));
            return;
        }

        ApexAdsInterstitialAdapter adapter = new ApexAdsInterstitialAdapter();
        adapter.context = config.getContext();

        adapter.interstitialAd = new InterstitialAd.Builder(placementId)
                .listener(new InterstitialAdListener() {
                    @Override public void onInterstitialLoaded() {
                        adapter.adCallback = loadCallback.onSuccess(adapter);
                    }
                    @Override public void onInterstitialFailed(@NonNull AdError error) {
                        loadCallback.onFailure(new com.google.android.gms.ads.AdError(
                                1, error.getMessage(), "ApexAds"));
                    }
                    @Override public void onInterstitialShown() {
                        if (adapter.adCallback != null) {
                            adapter.adCallback.onAdOpened();
                            adapter.adCallback.reportAdImpression();
                        }
                    }
                    @Override public void onInterstitialClicked() {
                        if (adapter.adCallback != null) adapter.adCallback.reportAdClicked();
                    }
                    @Override public void onInterstitialClosed() {
                        if (adapter.adCallback != null) adapter.adCallback.onAdClosed();
                    }
                })
                .build();

        adapter.interstitialAd.load();
    }

    @Override
    public void showAd(@NonNull Context context) {
        if (interstitialAd != null && interstitialAd.isReady()) {
            interstitialAd.show(context);
        } else {
            AdLog.w("ApexAdsInterstitialAdapter: showAd called but ad not ready");
        }
    }
}
