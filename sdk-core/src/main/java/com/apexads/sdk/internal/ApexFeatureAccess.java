package com.apexads.sdk.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.apexads.sdk.core.di.FeatureRegistry;
import com.apexads.sdk.core.di.SdkFeature;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ApexFeatureAccess {

    private static final FeatureRegistry pendingFeatures = new FeatureRegistry();
    private static volatile ApexServices services;

    private ApexFeatureAccess() {}

    public static synchronized void attach(@NonNull ApexServices createdServices) {
        createdServices.installFeaturesFrom(pendingFeatures);
        pendingFeatures.clear();
        services = createdServices;
    }

    public static synchronized void detach() {
        services = null;
        pendingFeatures.clear();
    }

    public static synchronized <T extends SdkFeature> void registerFeature(
            @NonNull Class<T> featureType,
            @NonNull T feature) {
        ApexServices current = services;
        if (current == null) {
            pendingFeatures.register(featureType, feature);
            return;
        }
        current.registerFeature(featureType, feature);
    }

    @Nullable
    public static <T extends SdkFeature> T getFeature(@NonNull Class<T> featureType) {
        ApexServices current = services;
        if (current != null) {
            return current.getFeature(featureType);
        }
        synchronized (ApexFeatureAccess.class) {
            return pendingFeatures.getOptional(featureType);
        }
    }

    public static boolean isFeatureRegistered(@NonNull Class<? extends SdkFeature> featureType) {
        return getFeature(featureType) != null;
    }
}
