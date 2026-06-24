package com.apexads.sdk.core.di;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FeatureRegistry {

    private final ConcurrentHashMap<Class<? extends SdkFeature>, SdkFeature> features =
            new ConcurrentHashMap<>();

    public <T extends SdkFeature> void register(@NonNull Class<T> featureType,
                                                @NonNull T feature) {
        registerStored(featureType, feature);
    }

    @Nullable
    public <T extends SdkFeature> T getOptional(@NonNull Class<T> featureType) {
        SdkFeature feature = features.get(featureType);
        return feature == null ? null : featureType.cast(feature);
    }

    @NonNull
    public <T extends SdkFeature> T getRequired(@NonNull Class<T> featureType) {
        T feature = getOptional(featureType);
        if (feature == null) {
            throw new IllegalStateException("No feature registered for "
                    + featureType.getSimpleName() + ".");
        }
        return feature;
    }

    public boolean isRegistered(@NonNull Class<? extends SdkFeature> featureType) {
        return features.containsKey(featureType);
    }

    public void copyFrom(@NonNull FeatureRegistry source) {
        for (Map.Entry<Class<? extends SdkFeature>, SdkFeature> entry : source.features.entrySet()) {
            registerStored(entry.getKey(), entry.getValue());
        }
    }

    public void clear() {
        features.clear();
    }

    private void registerStored(@NonNull Class<? extends SdkFeature> featureType,
                                @NonNull SdkFeature feature) {
        if (!featureType.isInstance(feature)) {
            throw new IllegalArgumentException(
                    feature.getClass().getSimpleName() + " does not implement "
                            + featureType.getSimpleName() + ".");
        }

        SdkFeature existing = features.putIfAbsent(featureType, feature);
        if (existing == null || existing == feature
                || existing.getClass().equals(feature.getClass())) {
            return;
        }

        throw new IllegalStateException(
                featureType.getSimpleName() + " is already registered by "
                        + existing.getClass().getSimpleName() + ".");
    }
}
