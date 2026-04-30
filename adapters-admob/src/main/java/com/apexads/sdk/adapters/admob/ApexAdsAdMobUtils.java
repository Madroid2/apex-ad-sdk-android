package com.apexads.sdk.adapters.admob;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.mediation.MediationAdConfiguration;
import com.google.android.gms.ads.mediation.MediationAdRequest;

/**
 * Shared helpers for extracting ApexAds placement IDs and network extras
 * from AdMob mediation configurations.
 */
public final class ApexAdsAdMobUtils {

    static final String KEY_PLACEMENT_ID = "placementId";
    static final String KEY_APP_TOKEN    = "appToken";

    private ApexAdsAdMobUtils() {}

    @Nullable
    public static String getPlacementId(@NonNull MediationAdConfiguration config) {
        String serverId = config.getServerParameters().getString(KEY_PLACEMENT_ID);
        if (serverId != null && !serverId.isEmpty()) return serverId;
        return null;
    }

    @Nullable
    public static String getAppToken(@NonNull MediationAdConfiguration config) {
        return config.getServerParameters().getString(KEY_APP_TOKEN);
    }

    @Nullable
    public static String getPlacementIdFromExtras(@Nullable Bundle extras) {
        if (extras == null) return null;
        return extras.getString(KEY_PLACEMENT_ID);
    }
}
