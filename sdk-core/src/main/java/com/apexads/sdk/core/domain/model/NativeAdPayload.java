package com.apexads.sdk.core.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/** Parsed IAB OpenRTB Native 1.2 asset payload. */
public final class NativeAdPayload {

    @NonNull  public final String title;
    @NonNull  public final String description;
    @Nullable public final String iconUrl;
    @Nullable public final String imageUrl;
    @NonNull  public final String ctaText;
    @Nullable public final String advertiserName;
    @Nullable public final String clickUrl;
    @NonNull  public final List<String> impressionTrackers;

    public NativeAdPayload(@NonNull String title,
                           @NonNull String description,
                           @Nullable String iconUrl,
                           @Nullable String imageUrl,
                           @NonNull String ctaText,
                           @Nullable String advertiserName,
                           @Nullable String clickUrl,
                           @NonNull List<String> impressionTrackers) {
        this.title = title;
        this.description = description;
        this.iconUrl = iconUrl;
        this.imageUrl = imageUrl;
        this.ctaText = ctaText;
        this.advertiserName = advertiserName;
        this.clickUrl = clickUrl;
        this.impressionTrackers = Collections.unmodifiableList(impressionTrackers);
    }
}
