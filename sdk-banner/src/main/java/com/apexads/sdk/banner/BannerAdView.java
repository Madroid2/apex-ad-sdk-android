package com.apexads.sdk.banner;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Backward-compatible publisher-facing banner view.
 *
 * <p>The implementation lives in the presentation layer; this class preserves
 * the existing {@code com.apexads.sdk.banner.BannerAdView} import path.
 */
public class BannerAdView extends com.apexads.sdk.banner.presentation.view.BannerAdView {

    public BannerAdView(@NonNull Context context) {
        super(context);
    }

    public BannerAdView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BannerAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
