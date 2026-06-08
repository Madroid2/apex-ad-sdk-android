package com.apexads.sdk.video;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.domain.repository.AdRepository;
import com.apexads.sdk.core.presentation.mvvm.AdViewModel;
import com.apexads.sdk.core.network.AdNetworkClient;
import com.apexads.sdk.core.utils.AdLog;
import com.apexads.sdk.video.presentation.view.VideoAdActivity;
import com.apexads.sdk.video.vast.VastParser;

public final class VideoAdViewModel extends AdViewModel {

    private static final String TAG = "VideoAdViewModel";

    private final VastParser vastParser;
    private final AdNetworkClient networkClient;

    @Nullable private VastParser.VastAd vastAd;

    VideoAdViewModel(
            @NonNull AdRepository repository,
            @NonNull AdCache cache,
            @NonNull String placementId,
            @NonNull VastParser vastParser,
            @NonNull AdNetworkClient networkClient) {
        super(repository, cache, AdFormat.REWARDED_VIDEO, AdSize.INTERSTITIAL_FULL, placementId, 0.0);
        this.vastParser = vastParser;
        this.networkClient = networkClient;
    }

    @Nullable
    public VastParser.VastAd getVastAd() {
        return vastAd;
    }

    public void show(@NonNull Context context, @NonNull VideoAdListener listener) {
        if (checkAndMarkExpired()) {
            AdLog.w(TAG + ": show() — ad expired");
            return;
        }
        if (vastAd == null) {
            AdLog.w(TAG + ": show() called before ad was loaded — ignored");
            return;
        }
        VideoAdActivity.launch(context, vastAd, networkClient, listener);
        onDisplayed();
    }

    @NonNull
    @Override
    protected AdData onAdLoaded(@NonNull AdData adData) throws AdError {
        String xml = adData.vastXml;
        if (xml == null || xml.isEmpty()) {
            throw new AdError.InvalidMarkup("VAST XML is empty");
        }

        VastParser.VastResult result = vastParser.parse(xml);
        if (!result.isSuccess()) {
            String msg = result.isNoFill ? "No fill" :
                    (result.errorMessage != null ? result.errorMessage : "VAST parse error");
            throw new AdError.InvalidMarkup(msg);
        }

        vastAd = result.ad;
        AdLog.i(TAG + ": VAST parsed adId=%s duration=%ds",
                result.ad.adId, result.ad.duration);
        return adData;
    }

    @Override
    public void destroy() {
        vastAd = null;
        super.destroy();
    }
}
