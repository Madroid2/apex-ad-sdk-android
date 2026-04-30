package com.apexads.sdk.nativeads;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.models.NativeAdPayload;
import com.apexads.sdk.core.network.AdNetworkClient;
import com.apexads.sdk.core.network.SdkExecutors;

import com.apexads.sdk.core.utils.AdLog;

/**
 * Flexible native ad container.
 *
 * The publisher supplies their own inner layout and registers views via
 * {@link #setTitleView}, {@link #setDescriptionView}, etc. The SDK binds
 * data assets and fires impression trackers — publisher retains full visual control.
 */
public final class NativeAdView extends FrameLayout {

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

    /** Called by {@link NativeAd#bindTo(NativeAdView)} — not publisher API. */
    void bind(@NonNull NativeAdPayload payload, @NonNull AdNetworkClient networkClient) {
        if (titleView != null)       titleView.setText(payload.title);
        if (descriptionView != null) descriptionView.setText(payload.description);
        if (ctaView != null)         ctaView.setText(payload.ctaText);
        if (advertiserView != null)  advertiserView.setText(payload.advertiserName);

        // Store image URLs as tags so publishers can pick them up with Glide/Coil/Picasso
        if (iconView != null)      iconView.setTag(payload.iconUrl);
        if (mainImageView != null) mainImageView.setTag(payload.imageUrl);

        String clickUrl = payload.clickUrl;
        if (clickUrl != null) {
            setOnClickListener(v -> openUrl(clickUrl));
            if (ctaView != null) ctaView.setOnClickListener(v -> openUrl(clickUrl));
        }

        // Fire impression trackers from background thread
        for (String url : payload.impressionTrackers) {
            SdkExecutors.IO.execute(() -> networkClient.fireTrackingUrl(url));
        }

        AdLog.d("NativeAdView: bound payload title='%s'", payload.title);
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        } catch (Exception e) {
            AdLog.w(e, "NativeAdView: could not open URL: %s", url);
        }
    }
}
