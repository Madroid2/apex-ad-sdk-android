package com.apexads.sdk.core.di;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.apexads.sdk.internal.ApexFeatureAccess;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ServiceLocator {

    private ServiceLocator() {}

    @NonNull
    public static <T extends SdkFeature> T get(@NonNull Class<T> featureType) {
        T feature = ApexFeatureAccess.getFeature(featureType);
        if (feature == null) {
            throw new IllegalStateException("No feature registered for "
                    + featureType.getSimpleName() + ".");
        }
        return feature;
    }

    @Nullable
    public static <T extends SdkFeature> T getOptional(@NonNull Class<T> featureType) {
        return ApexFeatureAccess.getFeature(featureType);
    }

    public static <T extends SdkFeature> void register(@NonNull Class<T> featureType,
                                                       @NonNull T feature) {
        ApexFeatureAccess.registerFeature(featureType, feature);
    }

    public static boolean isRegistered(@NonNull Class<? extends SdkFeature> featureType) {
        return ApexFeatureAccess.isFeatureRegistered(featureType);
    }
}
