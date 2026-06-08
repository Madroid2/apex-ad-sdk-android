package com.apexads.sdk.nativeads;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class NativeAdView extends com.apexads.sdk.nativeads.presentation.view.NativeAdView {

    public NativeAdView(@NonNull Context context) {
        super(context);
    }

    public NativeAdView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public NativeAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
