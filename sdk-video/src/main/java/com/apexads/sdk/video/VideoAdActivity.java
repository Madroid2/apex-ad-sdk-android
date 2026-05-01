package com.apexads.sdk.video;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.app.Activity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.apexads.sdk.core.network.AdNetworkClient;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.video.vast.VastParser;
import com.apexads.sdk.video.vast.VastParser.TrackingEvent;
import com.apexads.sdk.video.vast.VastParser.VastAd;

import java.util.List;

import com.apexads.sdk.core.utils.AdLog;

/**
 * Fullscreen video activity driven by a parsed {@link VastAd}.
 *
 * Manages ExoPlayer lifecycle, VAST quartile tracking, skip-button reveal,
 * click-through handling, and reward callback on completion.
 */
public final class VideoAdActivity extends Activity {

    // Static holder avoids Parcelable / Intent serialization of complex objects
    private static volatile VastAd           pendingAd;
    private static volatile AdNetworkClient  pendingNetworkClient;
    private static volatile VideoAdListener  activeListener;

    static void launch(@NonNull Context context,
                       @NonNull VastAd ad,
                       @NonNull AdNetworkClient networkClient,
                       @Nullable VideoAdListener listener) {
        pendingAd            = ad;
        pendingNetworkClient = networkClient;
        activeListener       = listener;
        Intent intent = new Intent(context, VideoAdActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private ExoPlayer       player;
    private PlayerView      playerView;
    private ImageButton     btnSkip;
    private TextView        tvSkipTimer;
    private TextView        tvAdLabel;

    private VastAd          vastAd;
    private AdNetworkClient networkClient;
    private VideoAdListener listener;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean startFired = false;
    private boolean q1Fired = false;
    private boolean midFired = false;
    private boolean q3Fired = false;
    private boolean completeFired = false;
    private boolean rewardGranted = false;
    private int skipCountdown;

    private final Runnable skipTickRunnable = new Runnable() {
        @Override public void run() {
            if (skipCountdown <= 0) {
                showSkipButton();
                return;
            }
            tvSkipTimer.setText("Skip in " + skipCountdown + "s");
            skipCountdown--;
            mainHandler.postDelayed(this, 1000);
        }
    };

    private final Runnable quartileRunnable = new Runnable() {
        @Override public void run() {
            if (player == null) return;
            checkQuartiles();
            mainHandler.postDelayed(this, 500);
        }
    };

    // ── Activity lifecycle ────────────────────────────────────────────────────

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Snapshot statics and clear immediately (prevent leaks on back-press before play)
        vastAd        = pendingAd;
        networkClient = pendingNetworkClient;
        listener      = activeListener;
        pendingAd            = null;
        pendingNetworkClient = null;
        // Keep activeListener until onDestroy so it can receive reward after activity finishes

        if (vastAd == null) {
            AdLog.e("VideoAdActivity: launched with null VastAd — finishing");
            finish();
            return;
        }

        makeFullscreen();
        setContentView(buildLayout());
        initPlayer();
        startSkipCountdown();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (player != null) player.setPlayWhenReady(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) player.setPlayWhenReady(false);
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(skipTickRunnable);
        mainHandler.removeCallbacks(quartileRunnable);
        releasePlayer();
        activeListener = null;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // Treat back-press as user-initiated skip if skip is not yet available
        if (vastAd != null && vastAd.skipOffset >= 0 && btnSkip.getVisibility() != View.VISIBLE) {
            return; // swallow — ad is not skippable yet
        }
        fireTracking(TrackingEvent.SKIP);
        if (listener != null) SdkExecutors.MAIN.post(() -> listener.onVideoAdSkipped());
        super.onBackPressed();
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private View buildLayout() {
        android.widget.FrameLayout root = new android.widget.FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        playerView = new PlayerView(this);
        playerView.setUseController(false); // SDK controls playback UI
        root.addView(playerView, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        // "Ad" label top-left
        tvAdLabel = new TextView(this);
        tvAdLabel.setText("Ad");
        tvAdLabel.setTextColor(0xCCFFFFFF);
        tvAdLabel.setTextSize(11f);
        tvAdLabel.setBackgroundColor(0x88000000);
        tvAdLabel.setPadding(dp(6), dp(3), dp(6), dp(3));
        android.widget.FrameLayout.LayoutParams adLabelParams =
                new android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
        adLabelParams.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
        adLabelParams.setMargins(dp(12), dp(12), 0, 0);
        root.addView(tvAdLabel, adLabelParams);

        // Skip timer / skip button — top-right
        tvSkipTimer = new TextView(this);
        tvSkipTimer.setTextColor(0xCCFFFFFF);
        tvSkipTimer.setTextSize(13f);
        tvSkipTimer.setBackgroundColor(0x88000000);
        tvSkipTimer.setPadding(dp(8), dp(4), dp(8), dp(4));
        android.widget.FrameLayout.LayoutParams timerParams =
                new android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
        timerParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
        timerParams.setMargins(0, dp(12), dp(12), 0);
        root.addView(tvSkipTimer, timerParams);

        btnSkip = new ImageButton(this);
        btnSkip.setImageResource(android.R.drawable.ic_media_next);
        btnSkip.setBackgroundColor(0xCC000000);
        btnSkip.setVisibility(View.GONE);
        btnSkip.setContentDescription("Skip ad");
        btnSkip.setPadding(dp(12), dp(8), dp(12), dp(8));
        android.widget.FrameLayout.LayoutParams skipParams =
                new android.widget.FrameLayout.LayoutParams(
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
        skipParams.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.END;
        skipParams.setMargins(0, 0, dp(12), dp(12));
        root.addView(btnSkip, skipParams);

        btnSkip.setOnClickListener(v -> {
            fireTracking(TrackingEvent.SKIP);
            if (listener != null) SdkExecutors.MAIN.post(() -> listener.onVideoAdSkipped());
            finish();
        });

        // Click-through on the player surface
        if (vastAd.clickThroughUrl != null) {
            String clickUrl = vastAd.clickThroughUrl;
            playerView.setOnClickListener(v -> {
                fireTrackingList(vastAd.clickTrackingUrls);
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(clickUrl));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    if (listener != null) SdkExecutors.MAIN.post(() -> listener.onVideoAdClicked());
                } catch (Exception e) {
                    AdLog.w(e, "VideoAdActivity: could not open click URL");
                }
            });
        }

        return root;
    }

    // ── Player ────────────────────────────────────────────────────────────────

    private void initPlayer() {
        VastParser.MediaFile media = vastAd.getBestMediaFile();
        if (media == null) {
            AdLog.e("VideoAdActivity: no playable media file in VastAd");
            finish();
            return;
        }

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        player.setMediaItem(MediaItem.fromUri(Uri.parse(media.url)));
        player.prepare();
        player.setPlayWhenReady(true);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) {
                    onAdComplete();
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying && !startFired) {
                    startFired = true;
                    fireTracking(TrackingEvent.START);
                    if (listener != null) SdkExecutors.MAIN.post(() -> listener.onVideoAdStarted());
                }
            }

            @Override
            public void onPositionDiscontinuity(
                    @NonNull Player.PositionInfo oldPosition,
                    @NonNull Player.PositionInfo newPosition,
                    int reason) {
                checkQuartiles();
            }
        });

        mainHandler.post(quartileRunnable);
    }

    private void checkQuartiles() {
        if (player == null) return;
        long pos      = player.getCurrentPosition();
        long duration = player.getDuration();
        if (duration <= 0) return;

        float ratio = (float) pos / duration;
        if (!q1Fired && ratio >= 0.25f) {
            q1Fired = true;
            fireTracking(TrackingEvent.FIRST_QUARTILE);
        }
        if (!midFired && ratio >= 0.50f) {
            midFired = true;
            fireTracking(TrackingEvent.MIDPOINT);
        }
        if (!q3Fired && ratio >= 0.75f) {
            q3Fired = true;
            fireTracking(TrackingEvent.THIRD_QUARTILE);
        }
    }

    private void onAdComplete() {
        if (completeFired) return;
        completeFired = true;
        fireTracking(TrackingEvent.COMPLETE);
        if (!rewardGranted) {
            rewardGranted = true;
            if (listener != null) SdkExecutors.MAIN.post(() -> {
                listener.onVideoAdCompleted();
                listener.onRewardEarned();
            });
        }
        mainHandler.postDelayed(this::finish, 300);
    }

    private void releasePlayer() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
    }

    // ── Skip logic ────────────────────────────────────────────────────────────

    private void startSkipCountdown() {
        if (vastAd.skipOffset < 0) {
            // Not skippable — hide both timer and button
            tvSkipTimer.setVisibility(View.GONE);
            return;
        }
        if (vastAd.skipOffset == 0) {
            showSkipButton();
            return;
        }
        skipCountdown = vastAd.skipOffset;
        mainHandler.post(skipTickRunnable);
    }

    private void showSkipButton() {
        tvSkipTimer.setVisibility(View.GONE);
        btnSkip.setVisibility(View.VISIBLE);
    }

    // ── VAST tracking ─────────────────────────────────────────────────────────

    private void fireTracking(@NonNull TrackingEvent event) {
        List<String> urls = vastAd.trackingEvents.get(event);
        if (urls != null) fireTrackingList(urls);
        AdLog.d("VideoAdActivity: fired tracking event=%s", event.name());
    }

    private void fireTrackingList(@NonNull List<String> urls) {
        for (String url : urls) {
            SdkExecutors.IO.execute(() -> networkClient.fireTrackingUrl(url));
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void makeFullscreen() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private int dp(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
