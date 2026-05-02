package com.apexads.sdk.video;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

import com.apexads.sdk.core.network.AdNetworkClient;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.video.vast.VastParser;
import com.apexads.sdk.video.vast.VastParser.TrackingEvent;
import com.apexads.sdk.video.vast.VastParser.VastAd;

import java.util.List;

import com.apexads.sdk.core.utils.AdLog;

/**
 * Fullscreen rewarded-video Activity driven by a parsed {@link VastAd}.
 *
 * Two independent timers run in parallel:
 * <ul>
 *   <li><b>Close timer</b> (top-right) — counts down {@link #CLOSE_DELAY_SECONDS} seconds,
 *       then reveals an X button. Back-press is swallowed until it appears.</li>
 *   <li><b>Skip timer</b> (bottom-right) — counts down {@link VastAd#skipOffset} seconds
 *       per the VAST spec, then reveals a Skip button. Only shown for skippable ads.</li>
 * </ul>
 *
 * Reward is granted only on video completion, not on close or skip.
 */
public final class VideoAdActivity extends Activity {
    // Static holder — avoids Parcelable / Intent serialization of complex objects
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

    private ExoPlayer    player;
    private PlayerView   playerView;

    // Close controls — top-right
    private TextView     tvCloseCountdown;
    private ImageButton  btnClose;

    // Skip controls — bottom-right (VAST skipOffset driven)
    private TextView     tvSkipTimer;
    private ImageButton  btnSkip;

    private VastAd          vastAd;
    private AdNetworkClient networkClient;
    private VideoAdListener listener;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // VAST quartile state
    private boolean startFired     = false;
    private boolean q1Fired        = false;
    private boolean midFired       = false;
    private boolean q3Fired        = false;
    private boolean completeFired  = false;
    private boolean rewardGranted  = false;

    // Timer state
    private int closeCountdown;
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

        // Snapshot statics; clear pendingAd immediately to prevent leaks on early exit
        vastAd        = pendingAd;
        networkClient = pendingNetworkClient;
        listener      = activeListener;
        pendingAd            = null;
        pendingNetworkClient = null;
        // Keep activeListener until onDestroy so reward fires even after finish()

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
        if (btnClose != null && btnClose.getVisibility() == View.VISIBLE) {
            // Close button available — exit without reward
            finish();
            return;
        }
        if (btnSkip != null && btnSkip.getVisibility() == View.VISIBLE) {
            // Skip button available (but close not yet) — skip without reward
            fireTracking(TrackingEvent.SKIP);
            if (listener != null) SdkExecutors.MAIN.post(() -> listener.onVideoAdSkipped());
            finish();
            return;
        }
        // Swallow — neither close nor skip is available yet
    }

    // ── Layout ────────────────────────────────────────────────────────────────

    private View buildLayout() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        // Player — full screen
        playerView = new PlayerView(this);
        playerView.setUseController(false);
        root.addView(playerView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        // "Ad" label — top-left
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

        // Close countdown badge — top-right, circular
        tvCloseCountdown = new TextView(this);
        tvCloseCountdown.setTextColor(Color.WHITE);
        tvCloseCountdown.setTextSize(14f);
        tvCloseCountdown.setTypeface(tvCloseCountdown.getTypeface(), Typeface.BOLD);
        tvCloseCountdown.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0xCC000000);
        tvCloseCountdown.setBackground(circle);
        FrameLayout.LayoutParams cdParams = new FrameLayout.LayoutParams(dp(40), dp(40));
        cdParams.gravity = Gravity.TOP | Gravity.END;
        cdParams.setMargins(0, dp(12), dp(12), 0);
        root.addView(tvCloseCountdown, cdParams);

        // Close button — top-right, hidden until countdown ends
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

        // Skip countdown text — bottom-right (only for skippable ads)
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

        // Skip button — bottom-right, revealed after skipOffset
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
                if (state == Player.STATE_ENDED) onAdComplete();
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
        if (!q1Fired  && ratio >= 0.25f) { q1Fired  = true; fireTracking(TrackingEvent.FIRST_QUARTILE); }
        if (!midFired && ratio >= 0.50f) { midFired = true; fireTracking(TrackingEvent.MIDPOINT); }
        if (!q3Fired  && ratio >= 0.75f) { q3Fired  = true; fireTracking(TrackingEvent.THIRD_QUARTILE); }
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
                showCloseButton();
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

    // ── Close / Skip logic ────────────────────────────────────────────────────

    private void showCloseButton() {
        tvCloseCountdown.setVisibility(View.GONE);
        btnClose.setVisibility(View.VISIBLE);
    }

    private void startSkipCountdown() {
        if (vastAd.skipOffset < 0) {
            // Not skippable — hide skip controls entirely
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
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
