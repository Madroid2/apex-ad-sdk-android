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
import com.apexads.sdk.core.models.IntentAction;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.tracking.TrackingClient;
import com.apexads.sdk.core.tracking.ImpressionTracker;

import com.apexads.sdk.core.utils.AdViewLifecycle;
import com.apexads.sdk.core.utils.AdLog;
import com.apexads.sdk.nativeads.NativeAd;
import com.apexads.sdk.nativeads.NativeAdClicks;

public class NativeAdView extends FrameLayout {

    private TextView titleView;
    private TextView descriptionView;
    private TextView ctaView;
    private TextView advertiserView;
    private TextView disclosureView;
    private TextView intentLabelView;
    private TextView actionBadgeView;
    private ImageView iconView;
    private ImageView mainImageView;
    @Nullable private NativeAdPayload boundPayload;
    @Nullable private TrackingClient boundTrackingClient;
    @Nullable private ImpressionTracker impressionTracker;

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
    public void setDisclosureView(@NonNull TextView view)  { disclosureView = view; }
    public void setIntentLabelView(@NonNull TextView view) { intentLabelView = view; }
    public void setActionBadgeView(@NonNull TextView view) { actionBadgeView = view; }

    public void bind(@NonNull NativeAdPayload payload, @NonNull TrackingClient trackingClient) {
        bind(payload, trackingClient, null, null, null);
    }

    public void bind(
            @NonNull NativeAdPayload payload,
            @NonNull TrackingClient trackingClient,
            @Nullable IntentAction action,
            @Nullable Runnable actionClick,
            @Nullable Runnable actionRendered) {
        if (impressionTracker != null) impressionTracker.detach();
        this.boundPayload = payload;
        this.boundTrackingClient = trackingClient;
        if (titleView != null)       titleView.setText(payload.title);
        if (descriptionView != null) descriptionView.setText(payload.description);
        if (ctaView != null)         ctaView.setText(action != null ? action.ctaText : payload.ctaText);
        if (advertiserView != null)  advertiserView.setText(payload.advertiserName);
        if (disclosureView != null)  disclosureView.setText(action != null ? action.disclosure : "Sponsored");
        if (intentLabelView != null) {
            intentLabelView.setText(action != null ? action.intentLabel : null);
            intentLabelView.setVisibility(action != null && action.intentLabel != null ? View.VISIBLE : View.GONE);
        }
        if (actionBadgeView != null) {
            actionBadgeView.setText(action != null ? action.badgeText : null);
            actionBadgeView.setVisibility(action != null && action.badgeText != null ? View.VISIBLE : View.GONE);
        }

        if (iconView != null)      iconView.setTag(payload.iconUrl);
        if (mainImageView != null) mainImageView.setTag(payload.imageUrl);

        setOnClickListener(null);
        if (ctaView != null) ctaView.setOnClickListener(null);
        if (payload.clickUrl != null) {
            setOnClickListener(v -> openClickThrough());
        }
        if (ctaView != null && actionClick != null) {
            ctaView.setOnClickListener(v -> actionClick.run());
        } else if (ctaView != null && payload.clickUrl != null) {
            ctaView.setOnClickListener(v -> openClickThrough());
        }

        impressionTracker = new ImpressionTracker(trackingClient);
        impressionTracker.attach(this, () -> {
            for (String url : payload.impressionTrackers) {
                SdkExecutors.IO.execute(() -> trackingClient.fireTrackingUrl(url));
            }
            if (actionRendered != null) actionRendered.run();
        }, null);

        AdLog.d("NativeAdView: bound payload title='%s'", payload.title);
    }

    /** Clears click listeners and asset-view refs set up by {@link #bind}. Idempotent. */
    public void destroy() {
        if (impressionTracker != null) impressionTracker.detach();
        impressionTracker = null;
        setOnClickListener(null);
        if (ctaView != null) ctaView.setOnClickListener(null);
        boundPayload = null;
        boundTrackingClient = null;

        titleView = null;
        descriptionView = null;
        ctaView = null;
        advertiserView = null;
        disclosureView = null;
        intentLabelView = null;
        actionBadgeView = null;
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

    private void openClickThrough() {
        NativeAdPayload payload = boundPayload;
        TrackingClient trackingClient = boundTrackingClient;
        if (payload == null || trackingClient == null) return;
        // Deep-link aware: web URLs open as before; app deep links launch the
        // app with link.fallback as the web fallback (see NativeAdClicks).
        NativeAdClicks.open(getContext(), payload, trackingClient, "NativeAdView");
    }
}
