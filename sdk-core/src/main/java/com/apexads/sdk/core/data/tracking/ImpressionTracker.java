package com.apexads.sdk.core.tracking;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;

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

    public ImpressionTracker(@NonNull AdNetworkClient networkClient) {
        this.networkClient = networkClient;
    }

    public void attach(@NonNull View view, @NonNull AdData adData) {
        if (fired) return;

        ViewTreeObserver.OnPreDrawListener preDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (!fired) {
                    checkVisibility(view, adData);
                }
                if (fired && view.getViewTreeObserver().isAlive()) {
                    view.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                return true;
            }
        };

        view.getViewTreeObserver().addOnPreDrawListener(preDrawListener);

        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(@NonNull View v) {}

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                if (v.getViewTreeObserver().isAlive()) {
                    v.getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
                }
                v.removeOnAttachStateChangeListener(this);
            }
        });
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
