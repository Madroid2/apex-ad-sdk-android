package com.apexads.sdk.core.tracking;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.models.EngagementMeasurement;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.utils.AdLog;

import java.util.List;

/** Emits only server-provided milestone URLs while the ad is actually viewable. */
public final class EngagementTracker {

    private final TrackingClient trackingClient;
    private final EngagementMeasurement measurement;
    private final EngagementAccumulator accumulator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Rect visibleRect = new Rect();
    @Nullable private View view;
    private boolean active;
    private boolean stopped;

    private final Runnable sampleRunnable = new Runnable() {
        @Override public void run() {
            if (!active || stopped || view == null) return;
            dispatch(accumulator.sample(SystemClock.elapsedRealtime(), isViewable(view)));
            handler.postDelayed(this, measurement.sampleIntervalMs);
        }
    };

    public EngagementTracker(@NonNull TrackingClient trackingClient,
                             @NonNull EngagementMeasurement measurement,
                             @NonNull String surface) {
        this.trackingClient = trackingClient;
        this.measurement = measurement;
        this.accumulator = new EngagementAccumulator(measurement, surface);
    }

    public void attach(@NonNull View adView) {
        if (stopped) return;
        view = adView;
        resume();
    }

    public void resume() {
        if (stopped || view == null || active) return;
        active = true;
        handler.removeCallbacks(sampleRunnable);
        handler.post(sampleRunnable);
    }

    public void pause() {
        active = false;
        handler.removeCallbacks(sampleRunnable);
        accumulator.pause();
    }

    public void fireSignal(@NonNull String signal) {
        if (stopped) return;
        dispatch(accumulator.signal(signal));
    }

    public void stop() {
        if (stopped) return;
        pause();
        stopped = true;
        view = null;
    }

    private boolean isViewable(@NonNull View adView) {
        if (!adView.isShown() || !adView.hasWindowFocus() ||
                adView.getWindowVisibility() != View.VISIBLE ||
                !adView.getGlobalVisibleRect(visibleRect)) {
            return false;
        }
        long totalArea = (long) adView.getWidth() * adView.getHeight();
        if (totalArea <= 0L) return false;
        long visibleArea = (long) visibleRect.width() * visibleRect.height();
        return (float) visibleArea / totalArea >= measurement.minVisibleRatio;
    }

    private void dispatch(@NonNull List<String> urls) {
        for (String url : urls) {
            AdLog.d("EngagementTracker: firing signed milestone");
            SdkExecutors.IO.execute(() -> trackingClient.fireTrackingUrl(url));
        }
    }
}
