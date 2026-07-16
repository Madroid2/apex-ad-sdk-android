package com.apexads.sdk.video.presentation.view;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.app.Activity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.apexads.sdk.core.utils.AdUrlHandler;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.tracking.TrackingClient;
import com.apexads.sdk.video.VideoAdListener;
import com.apexads.sdk.video.vast.VastParser;
import com.apexads.sdk.video.vast.VastParser.TrackingEvent;
import com.apexads.sdk.video.vast.VastParser.VastAd;

import java.util.List;

import com.apexads.sdk.core.utils.AdLog;

public final class VideoAdActivity extends Activity {

    private static volatile VastAd          pendingAd;
    private static volatile TrackingClient  pendingTrackingClient;
    private static volatile VideoAdListener activeListener;

    public static void launch(@NonNull Context context,
                              @NonNull VastAd ad,
                              @NonNull TrackingClient trackingClient,
                              @Nullable VideoAdListener listener) {
        pendingAd             = ad;
        pendingTrackingClient = trackingClient;
        activeListener        = listener;
        Intent intent = new Intent(context, VideoAdActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private ExoPlayer    player;
    private PlayerView   playerView;

    private ImageButton  btnClose;

    private TextView     tvSkipTimer;
    private ImageButton  btnSkip;

    private VastAd vastAd;
    private TrackingClient trackingClient;
    private VideoAdListener listener;

    @Nullable private Player.Listener playerListener;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private boolean startFired     = false;
    private boolean q1Fired        = false;
    private boolean midFired       = false;
    private boolean q3Fired        = false;
    private boolean completeFired  = false;
    private boolean rewardGranted  = false;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        vastAd         = pendingAd;
        trackingClient = pendingTrackingClient;
        listener       = activeListener;
        pendingAd             = null;
        pendingTrackingClient = null;

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
        listener = null;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (btnClose != null && btnClose.getVisibility() == View.VISIBLE) {

            finish();
            return;
        }
        if (btnSkip != null && btnSkip.getVisibility() == View.VISIBLE) {

            fireTracking(TrackingEvent.SKIP);
            notifyListener(VideoAdListener::onVideoAdSkipped);
            finish();
            return;
        }

    }

    private View buildLayout() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        playerView = new PlayerView(this);
        playerView.setUseController(false);
        root.addView(playerView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        TextView tvAdLabel = new TextView(this);
        tvAdLabel.setText("Ad");
        tvAdLabel.setTextColor(0xCCFFFFFF);
        tvAdLabel.setTextSize(11f);
        tvAdLabel.setBackgroundColor(0x88000000);
        tvAdLabel.setPadding(dp(6), dp(3), dp(6), dp(3));
        FrameLayout.LayoutParams adLabelParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        adLabelParams.gravity = Gravity.TOP | Gravity.START;
        adLabelParams.setMargins(dp(12), dp(12), 0, 0);
        root.addView(tvAdLabel, adLabelParams);

        btnClose = new ImageButton(this);
        btnClose.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnClose.setBackgroundColor(0xCC000000);
        btnClose.setContentDescription("Close ad");
        btnClose.setPadding(dp(8), dp(8), dp(8), dp(8));
        btnClose.setVisibility(View.GONE);
        btnClose.setOnClickListener(v -> finish());
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(dp(44), dp(44));
        closeParams.gravity = Gravity.TOP | Gravity.END;
        closeParams.setMargins(0, dp(10), dp(10), 0);
        root.addView(btnClose, closeParams);

        tvSkipTimer = new TextView(this);
        tvSkipTimer.setTextColor(0xCCFFFFFF);
        tvSkipTimer.setTextSize(13f);
        tvSkipTimer.setBackgroundColor(0x88000000);
        tvSkipTimer.setPadding(dp(8), dp(4), dp(8), dp(4));
        FrameLayout.LayoutParams timerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        timerParams.gravity = Gravity.BOTTOM | Gravity.END;
        timerParams.setMargins(0, 0, dp(12), dp(12));
        root.addView(tvSkipTimer, timerParams);

        btnSkip = new ImageButton(this);
        btnSkip.setImageResource(android.R.drawable.ic_media_next);
        btnSkip.setBackgroundColor(0xCC000000);
        btnSkip.setVisibility(View.GONE);
        btnSkip.setContentDescription("Skip ad");
        btnSkip.setPadding(dp(12), dp(8), dp(12), dp(8));
        FrameLayout.LayoutParams skipParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        skipParams.gravity = Gravity.BOTTOM | Gravity.END;
        skipParams.setMargins(0, 0, dp(12), dp(12));
        root.addView(btnSkip, skipParams);

        btnSkip.setOnClickListener(v -> {
            fireTracking(TrackingEvent.SKIP);
            notifyListener(VideoAdListener::onVideoAdSkipped);
            finish();
        });

        if (vastAd.clickThroughUrl != null) {
            String clickUrl = vastAd.clickThroughUrl;
            playerView.setOnClickListener(v -> {
                fireTrackingList(vastAd.clickTrackingUrls);
                // Deep-link aware: a VAST ClickThrough may be an app deep link
                // (custom scheme / market://) as well as a web URL.
                if (AdUrlHandler.openClickThrough(this, clickUrl, null, "VideoAdActivity")
                        != AdUrlHandler.OPEN_FAILED) {
                    notifyListener(VideoAdListener::onVideoAdClicked);
                }
            });
        }

        return root;
    }

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

        playerListener = new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_ENDED) onAdComplete();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying && !startFired) {
                    startFired = true;
                    fireTracking(TrackingEvent.START);
                    notifyListener(VideoAdListener::onVideoAdStarted);
                }
            }

            @Override
            public void onPositionDiscontinuity(
                    @NonNull Player.PositionInfo oldPosition,
                    @NonNull Player.PositionInfo newPosition,
                    int reason) {
                checkQuartiles();
            }
        };
        player.addListener(playerListener);

        mainHandler.post(quartileRunnable);
    }

    private void checkQuartiles() {
        if (player == null) return;
        long pos      = player.getCurrentPosition();
        long duration = player.getDuration();
        if (duration <= 0) return;

        float ratio = (float) pos / duration;
        if (!q1Fired  && ratio >= 0.25f) { q1Fired  = true; fireTracking(TrackingEvent.FIRST_QUARTILE); }
        if (!midFired && ratio >= 0.50f) { midFired = true; fireTracking(TrackingEvent.MIDPOINT); }
        if (!q3Fired  && ratio >= 0.75f) { q3Fired  = true; fireTracking(TrackingEvent.THIRD_QUARTILE); }
    }

    private void onAdComplete() {
        if (completeFired) return;
        completeFired = true;
        fireTracking(TrackingEvent.COMPLETE);

        showCloseButton();
        if (!rewardGranted) {
            rewardGranted = true;
            notifyListener(l -> { l.onVideoAdCompleted(); l.onRewardEarned(); });
        }
    }

    /** Posts to the listener only if it's still set, snapshotting it to avoid NPEs/ghost calls. */
    private void notifyListener(@NonNull java.util.function.Consumer<VideoAdListener> action) {
        VideoAdListener snapshot = listener;
        if (snapshot != null) {
            SdkExecutors.MAIN.post(() -> action.accept(snapshot));
        }
    }

    private void releasePlayer() {
        if (player != null) {
            // release() doesn't guarantee listener removal — remove it explicitly.
            if (playerListener != null) {
                player.removeListener(playerListener);
                playerListener = null;
            }
            player.stop();
            player.release();
            player = null;
        }
    }

    private void showCloseButton() {
        btnClose.setVisibility(View.VISIBLE);
    }

    private void startSkipCountdown() {
        if (vastAd.skipOffset < 0) {

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

    private void fireTracking(@NonNull TrackingEvent event) {
        List<String> urls = vastAd.trackingEvents.get(event);
        if (urls != null) fireTrackingList(urls);
        AdLog.d("VideoAdActivity: fired tracking event=%s", event.name());
    }

    private void fireTrackingList(@NonNull List<String> urls) {
        for (String url : urls) {
            SdkExecutors.IO.execute(() -> trackingClient.fireTrackingUrl(url));
        }
    }

    private void makeFullscreen() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
