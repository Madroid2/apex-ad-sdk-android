package com.apexads.sdk.core.tracking;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.network.AdNetworkClient;
import com.apexads.sdk.core.network.SdkExecutors;

import com.apexads.sdk.core.utils.AdLog;

public final class ImpressionTracker {

    public static final float  MRC_MIN_VISIBLE_RATIO = 0.50f;

    public static final long   MRC_MIN_DURATION_MS   = 1_000L;

    private final AdNetworkClient networkClient;
    private final Rect checkRect = new Rect();
    private boolean fired = false;
    private long visibleStart = 0L;

    @Nullable private ViewTreeObserver.OnPreDrawListener preDrawListener;
    @Nullable private View.OnAttachStateChangeListener attachStateListener;
    @Nullable private View attachedView;

    public ImpressionTracker(@NonNull AdNetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    public void attach(@NonNull View view, @NonNull AdData adData) {
        if (fired) return;

        // Tear down any previous attachment first — guards against leaking listeners
        // onto a stale view if attach() is called again (e.g. ad refresh) without an
        // explicit detach()/destroy() in between.
        detach();

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

    /**
     * Removes both the {@link ViewTreeObserver.OnPreDrawListener} and the
     * {@link View.OnAttachStateChangeListener} registered in {@link #attach}, releasing
     * the strong references they hold to the bound View/AdData. Must be invoked whenever
     * the owning ad view is destroyed/recycled — otherwise a tracker that never reaches
     * the MRC visibility threshold keeps its pre-draw closure (and the View it captures)
     * alive for the remainder of the ViewTreeObserver's lifetime ("ghost" visibility checks).
     */
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
        final String winUrl = adData.winNoticeUrl;
        if (winUrl != null) {
            SdkExecutors.IO.execute(() -> networkClient.fireTrackingUrl(winUrl));
        }
    }

    public void reset() {
        fired = false;
        visibleStart = 0;
    }
}
