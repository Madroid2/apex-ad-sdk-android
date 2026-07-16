package com.apexads.sdk.core.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public final class NativeAdPayload {

    @NonNull  public final String title;
    @NonNull  public final String description;
    @Nullable public final String iconUrl;
    @Nullable public final String imageUrl;
    @NonNull  public final String ctaText;
    @Nullable public final String advertiserName;
    @Nullable public final String clickUrl;
    /**
     * Web landing to use when {@link #clickUrl} is an app deep link that cannot
     * be opened on this device (OpenRTB Native 1.2 {@code link.fallback}).
     */
    @Nullable public final String fallbackUrl;
    @NonNull  public final List<String> impressionTrackers;
    /**
     * OpenRTB Native 1.2 {@code link.clicktrackers}. Fired by the SDK when the
     * click-through opens a destination that bypasses server-side click
     * redirects (i.e. a deep link).
     */
    @NonNull  public final List<String> clickTrackers;

    public NativeAdPayload(@NonNull String title,
                           @NonNull String description,
                           @Nullable String iconUrl,
                           @Nullable String imageUrl,
                           @NonNull String ctaText,
                           @Nullable String advertiserName,
                           @Nullable String clickUrl,
                           @Nullable String fallbackUrl,
                           @NonNull List<String> impressionTrackers,
                           @NonNull List<String> clickTrackers) {
        this.title = title;
        this.description = description;
        this.iconUrl = iconUrl;
        this.imageUrl = imageUrl;
        this.ctaText = ctaText;
        this.advertiserName = advertiserName;
        this.clickUrl = clickUrl;
        this.fallbackUrl = fallbackUrl;
        this.impressionTrackers = Collections.unmodifiableList(impressionTrackers);
        this.clickTrackers = Collections.unmodifiableList(clickTrackers);
    }
}
