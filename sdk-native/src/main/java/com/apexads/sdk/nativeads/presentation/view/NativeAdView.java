package com.apexads.sdk.nativeads.presentation.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.models.NativeAdPayload;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.tracking.TrackingClient;

import com.apexads.sdk.core.utils.AdUrlHandler;
import com.apexads.sdk.core.utils.AdViewLifecycle;
import com.apexads.sdk.core.utils.AdLog;
import com.apexads.sdk.nativeads.NativeAd;

public class NativeAdView extends FrameLayout {

    private TextView titleView;
    private TextView descriptionView;
    private TextView ctaView;
    private TextView advertiserView;
    private ImageView iconView;
    private ImageView mainImageView;

    public NativeAdView(@NonNull Context context) { this(context, null); }

    public NativeAdView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NativeAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setTitleView(@NonNull TextView view)       { titleView = view; }
    public void setDescriptionView(@NonNull TextView view) { descriptionView = view; }
    public void setCtaView(@NonNull TextView view)         { ctaView = view; }
    public void setAdvertiserView(@NonNull TextView view)  { advertiserView = view; }
    public void setIconView(@NonNull ImageView view)       { iconView = view; }
    public void setMainImageView(@NonNull ImageView view)  { mainImageView = view; }

    public void bind(@NonNull NativeAdPayload payload, @NonNull TrackingClient trackingClient) {
        if (titleView != null)       titleView.setText(payload.title);
        if (descriptionView != null) descriptionView.setText(payload.description);
        if (ctaView != null)         ctaView.setText(payload.ctaText);
        if (advertiserView != null)  advertiserView.setText(payload.advertiserName);

        if (iconView != null)      iconView.setTag(payload.iconUrl);
        if (mainImageView != null) mainImageView.setTag(payload.imageUrl);

        String clickUrl = payload.clickUrl;
        if (clickUrl != null) {
            setOnClickListener(v -> openUrl(clickUrl));
            if (ctaView != null) ctaView.setOnClickListener(v -> openUrl(clickUrl));
        }

        for (String url : payload.impressionTrackers) {
            SdkExecutors.IO.execute(() -> trackingClient.fireTrackingUrl(url));
        }

        AdLog.d("NativeAdView: bound payload title='%s'", payload.title);
    }

    /** Clears click listeners and asset-view refs set up by {@link #bind}. Idempotent. */
    public void destroy() {
        setOnClickListener(null);
        if (ctaView != null) ctaView.setOnClickListener(null);

        titleView = null;
        descriptionView = null;
        ctaView = null;
        advertiserView = null;
        iconView = null;
        mainImageView = null;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (AdViewLifecycle.isTerminalDetach(this)) {
            destroy();
        } else {
            AdLog.d("NativeAdView: transient detach — retaining bound asset views");
        }
    }

    private void openUrl(String url) {
        AdUrlHandler.openExternalUrl(getContext(), url, "NativeAdView");
    }
}
