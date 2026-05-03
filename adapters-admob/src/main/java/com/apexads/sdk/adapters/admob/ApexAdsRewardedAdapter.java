package com.apexads.sdk.adapters.admob;

import android.content.Context;

import androidx.annotation.NonNull;

import com.apexads.sdk.video.VideoAd;
import com.apexads.sdk.video.VideoAdListener;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.utils.AdLog;
import com.google.android.gms.ads.mediation.MediationAdLoadCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAd;
import com.google.android.gms.ads.mediation.MediationRewardedAdCallback;
import com.google.android.gms.ads.mediation.MediationRewardedAdConfiguration;
import com.google.android.gms.ads.rewarded.RewardItem;

/**
 * AdMob mediation adapter — Rewarded Video.
 */
public final class ApexAdsRewardedAdapter implements MediationRewardedAd {

    private VideoAd videoAd;
    private MediationRewardedAdCallback adCallback;

    public ApexAdsRewardedAdapter() {}

    public static void load(
            @NonNull MediationRewardedAdConfiguration config,
            @NonNull MediationAdLoadCallback<MediationRewardedAd, MediationRewardedAdCallback> loadCallback) {

        String placementId = ApexAdsAdMobUtils.getPlacementId(config);
        if (placementId == null) {
            loadCallback.onFailure(new com.google.android.gms.ads.AdError(
                    0, "Missing placementId in server parameters", "ApexAds"));
            return;
        }

        ApexAdsRewardedAdapter adapter = new ApexAdsRewardedAdapter();

        adapter.videoAd = new VideoAd.Builder(placementId)
                .listener(new VideoAdListener() {
                    @Override public void onVideoAdLoaded() {
                        adapter.adCallback = loadCallback.onSuccess(adapter);
                    }
                    @Override public void onVideoAdFailed(@NonNull AdError error) {
                        loadCallback.onFailure(new com.google.android.gms.ads.AdError(
                                1, error.getMessage(), "ApexAds"));
                    }
                    @Override public void onVideoAdStarted() {
                        if (adapter.adCallback != null) {
                            adapter.adCallback.onAdOpened();
                            adapter.adCallback.onVideoStart();
                            adapter.adCallback.reportAdImpression();
                        }
                    }
                    @Override public void onVideoAdCompleted() {
                        if (adapter.adCallback != null) adapter.adCallback.onVideoComplete();
                    }
                    @Override public void onRewardEarned() {
                        if (adapter.adCallback != null) {
                            adapter.adCallback.onUserEarnedReward(new RewardItem() {
                                @NonNull @Override public String getType() { return ""; }
                                @Override public int getAmount() { return 1; }
                            });
                        }
                    }
                    @Override public void onVideoAdSkipped() {
                        if (adapter.adCallback != null) adapter.adCallback.onAdClosed();
                    }
                    @Override public void onVideoAdClicked() {
                        if (adapter.adCallback != null) adapter.adCallback.reportAdClicked();
                    }
                })
                .build();

        adapter.videoAd.load();
    }

    @Override
    public void showAd(@NonNull Context context) {
        if (videoAd != null && videoAd.isReady()) {
            videoAd.show(context);
        } else {
            AdLog.w("ApexAdsRewardedAdapter: showAd called but ad not ready");
        }
    }
}
