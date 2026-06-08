package com.apexads.sdk.core.domain.repository;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;

public interface AdRepository {

    interface OnSuccess {
        void onSuccess(@NonNull AdData adData);
    }

    interface OnFailure {
        void onFailure(@NonNull AdError error);
    }

    void loadAd(
            @NonNull AdFormat format,
            @NonNull AdSize size,
            @NonNull String placementId,
            double bidFloor,
            @NonNull OnSuccess onSuccess,
            @NonNull OnFailure onFailure
    );
}
