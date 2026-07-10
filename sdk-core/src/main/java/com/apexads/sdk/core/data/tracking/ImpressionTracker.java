package com.apexads.sdk.core.tracking;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.tracking.TrackingClient;

import com.apexads.sdk.core.utils.AdLog;

public final class ImpressionTracker {

    public static final float  MRC_MIN_VISIBLE_RATIO = 0.50f;

    public static final long   MRC_MIN_DURATION_MS   = 1_000L;

    private final TrackingClient trackingClient;
    private final Rect checkRect = new Rect();
    private boolean fired = false;
    private long visibleStart = 0L;

    @Nullable private Runnable onFiredCallback;
    @Nullable private ViewTreeObserver.OnPreDrawListener preDrawListener;
    @Nullable private View.OnAttachStateChangeListener attachStateListener;
    @Nullable private View attachedView;

    public ImpressionTracker(@NonNull TrackingClient trackingClient) {
        this.trackingClient = trackingClient;
    }

    public void attach(@NonNull View view, @NonNull AdData adData) {
        attach(view, adData, null);
    }

    public void attach(@NonNull View view, @NonNull AdData adData, @Nullable Runnable onFired) {
        if (fired) return;

        this.onFiredCallback = onFired;
        detach(); // release any previous attachment first

        attachedView = view;

        preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (!fired) {
                    checkVisibility(view, adData);
                }
                if (fired) {
                    detach();
                }
                return true;
            }
        };

        attachStateListener = new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(@NonNull View v) {}

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                detach();
            }
        };

        view.getViewTreeObserver().addOnPreDrawListener(preDrawListener);
        view.addOnAttachStateChangeListener(attachStateListener);
    }

    /** Removes the pre-draw and attach-state listeners, releasing the bound View. */
    public void detach() {
        if (attachedView != null) {
            View view = attachedView;
            if (preDrawListener != null && view.getViewTreeObserver().isAlive()) {
                view.getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
            }
            if (attachStateListener != null) {
                view.removeOnAttachStateChangeListener(attachStateListener);
            }
        }
        preDrawListener = null;
        attachStateListener = null;
        attachedView = null;
    }

    private void checkVisibility(View view, AdData adData) {
        if (!view.isShown()) {
            visibleStart = 0;
            return;
        }

        boolean visible = view.getGlobalVisibleRect(checkRect);
        if (!visible) {
            visibleStart = 0;
            return;
        }

        long visibleArea = (long) checkRect.width() * checkRect.height();
        long totalArea   = (long) view.getWidth()   * view.getHeight();
        if (totalArea == 0) return;

        float ratio = (float) visibleArea / totalArea;
        if (ratio < MRC_MIN_VISIBLE_RATIO) {
            visibleStart = 0;
            return;
        }

        long now = System.currentTimeMillis();
        if (visibleStart == 0) {
            visibleStart = now;
            return;
        }

        if (now - visibleStart >= MRC_MIN_DURATION_MS) {
            fireImpression(adData);
        }
    }

    private void fireImpression(AdData adData) {
        if (fired) return;
        fired = true;
        AdLog.d("ImpressionTracker: firing impression bid=%s", adData.bidId);
        if (onFiredCallback != null) {
            onFiredCallback.run();
        }
        final String winUrl = adData.winNoticeUrl;
        if (winUrl != null) {
            SdkExecutors.IO.execute(() -> trackingClient.fireTrackingUrl(winUrl));
        }
        // OpenRTB burl — the billable-impression notice. Fired at the same
        // MRC-viewable moment as nurl; DSPs that bill on burl (rather than on
        // win) depend on this, and DSPs that bill on either de-duplicate.
        final String billingUrl = adData.billingNoticeUrl;
        if (billingUrl != null) {
            SdkExecutors.IO.execute(() -> trackingClient.fireTrackingUrl(billingUrl));
        }
    }

    public void reset() {
        fired = false;
        visibleStart = 0;
    }
}
