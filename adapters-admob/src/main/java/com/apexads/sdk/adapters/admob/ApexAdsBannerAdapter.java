package com.apexads.sdk.adapters.admob;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import com.apexads.sdk.banner.BannerAd;
import com.apexads.sdk.banner.BannerAdListener;
import com.apexads.sdk.banner.BannerAdView;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdSize;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationBannerAd;
import com.google.android.gms.ads.mediation.MediationBannerAdCallback;
import com.google.android.gms.ads.mediation.MediationBannerAdConfiguration;

/**
 * AdMob mediation adapter — Banner.
 *
 * Configure in AdMob dashboard:
 *   Network: ApexAds
 *   Class name: com.apexads.sdk.adapters.admob.ApexAdsBannerAdapter
 *   Parameter: {"placementId":"your-placement-id","appToken":"your-app-token"}
 */
public final class ApexAdsBannerAdapter implements MediationBannerAd {

    private BannerAdView bannerAdView;
    private BannerAd bannerAd;
    private MediationBannerAdCallback adCallback;

    public ApexAdsBannerAdapter() {}

    public static void load(
            @NonNull MediationBannerAdConfiguration config,
            @NonNull MediationAdLoadCallback<MediationBannerAd, MediationBannerAdCallback> loadCallback) {

        String placementId = ApexAdsAdMobUtils.getPlacementId(config);
        if (placementId == null) {
            loadCallback.onFailure(new com.google.android.gms.ads.AdError(
                    0, "Missing placementId in server parameters", "ApexAds"));
            return;
        }

        Context context = config.getContext();
        ApexAdsBannerAdapter adapter = new ApexAdsBannerAdapter();
        adapter.bannerAdView = new BannerAdView(context);

        adapter.bannerAd = new BannerAd.Builder(placementId)
                .adSize(AdSize.BANNER_320x50)
                .listener(new BannerAdListener() {
                    @Override public void onAdLoaded() {
                        adapter.bannerAd.show(adapter.bannerAdView);
                        adapter.adCallback = loadCallback.onSuccess(adapter);
                    }
                    @Override public void onAdFailed(@NonNull AdError error) {
                        loadCallback.onFailure(new com.google.android.gms.ads.AdError(
                                1, error.getMessage(), "ApexAds"));
                    }
                    @Override public void onAdClicked() {
                        if (adapter.adCallback != null) {
                            adapter.adCallback.reportAdClicked();
                            adapter.adCallback.onAdOpened();
                        }
                    }
                    @Override public void onAdImpression() {
                        if (adapter.adCallback != null) adapter.adCallback.reportAdImpression();
                    }
                    @Override public void onAdExpanded() {}
                    @Override public void onAdClosed() {
                        if (adapter.adCallback != null) adapter.adCallback.onAdClosed();
                    }
                })
                .build();
        adapter.bannerAd.load();
    }

    @NonNull
    @Override
    public View getView() {
        return bannerAdView;
    }
}
