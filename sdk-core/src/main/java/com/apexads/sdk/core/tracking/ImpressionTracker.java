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

    /** Notified at most once, on the main thread, when the MRC-viewable impression fires. */
    public interface Listener {
        void onImpressionFired();
    }

    private final TrackingClient trackingClient;
    private final Rect checkRect = new Rect();
    private boolean fired = false;
    private long visibleStart = 0L;

    @Nullable private Listener listener;
    @Nullable private ViewTreeObserver.OnPreDrawListener preDrawListener;
    @Nullable private View.OnAttachStateChangeListener attachStateListener;
    @Nullable private View attachedView;

    public ImpressionTracker(@NonNull TrackingClient trackingClient) {
        this.trackingClient = trackingClient;
    }

    public void attach(@NonNull View view, @NonNull AdData adData) {
        attach(view, adData, null);
    }

    public void attach(@NonNull View view, @NonNull AdData adData,
                       @Nullable Listener impressionListener) {
        attach(view, () -> fireTrackingUrls(adData), impressionListener);
    }

    /**
     * Attaches the shared MRC visibility gate to an arbitrary impression action.
     * Native rendering uses this overload so imptrackers fire only after the same
     * 50%-for-one-second condition as banners.
     */
    public void attach(@NonNull View view, @NonNull Runnable impressionAction,
                       @Nullable Listener impressionListener) {
        if (fired) return;

        detach(); // release any previous attachment first

        listener = impressionListener;
        attachedView = view;

        preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (!fired) {
                    checkVisibility(view, impressionAction);
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

    private void checkVisibility(View view, Runnable impressionAction) {
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
            fireImpression(impressionAction);
        }
    }

    private void fireImpression(Runnable impressionAction) {
        if (fired) return;
        fired = true;
        impressionAction.run();
        if (listener != null) {
            listener.onImpressionFired();
        }
    }

    private void fireTrackingUrls(AdData adData) {
        AdLog.d("ImpressionTracker: firing impression bid=%s", adData.bidId);
        final String winUrl = adData.winNoticeUrl;
        if (winUrl != null) {
            SdkExecutors.IO.execute(() -> trackingClient.fireTrackingUrl(winUrl));
        }
        // burl is the billable event and must fire at the MRC-viewable impression,
        // separately from the win notice — exchanges reconcile billing off it.
        final String billingUrl = adData.billingUrl;
        if (billingUrl != null) {
            SdkExecutors.IO.execute(() -> trackingClient.fireTrackingUrl(billingUrl));
        }
    }

    public void reset() {
        fired = false;
        visibleStart = 0;
    }
}
