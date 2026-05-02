package com.apexads.sdk.interstitial;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.banner.mraid.MRAIDBridge;
import com.apexads.sdk.core.di.ServiceLocator;
import com.apexads.sdk.core.di.WalletDelegate;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.utils.AdLog;

/**
 * Fullscreen Activity that hosts the interstitial WebView.
 *
 * A countdown badge appears in the top-right corner for the first
 * {@link #CLOSE_DELAY_SECONDS} seconds, after which it is replaced by an
 * X close button — matching the behaviour of production interstitial SDKs.
 * Back-press is swallowed while the countdown is running.
 */
public final class InterstitialActivity extends Activity {

    /** Seconds before the close button becomes tappable. */
    private static final int CLOSE_DELAY_SECONDS = 5;

    // Static slots — cleared in onDestroy
    private static volatile AdData pendingAdData;
    private static volatile InterstitialAdListener activeListener;

    private WebView webView;
    private MRAIDBridge mraidBridge;
    private TextView tvCountdown;
    private ImageButton btnClose;
    private AdData adData;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private int closeCountdown = CLOSE_DELAY_SECONDS;

    private final Runnable closeTickRunnable = new Runnable() {
        @Override public void run() {
            if (closeCountdown <= 0) {
                showCloseButton();
                return;
            }
            tvCountdown.setText(String.valueOf(closeCountdown));
            closeCountdown--;
            mainHandler.postDelayed(this, 1000);
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        adData = pendingAdData;
        if (adData == null) { finish(); return; }

        window();

        FrameLayout root = new FrameLayout(this);

        // WebView — full screen
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);

        mraidBridge = new MRAIDBridge(new MRAIDBridge.MRAIDListener() {
            @Override public void onClose()                                          { finish(); }
            @Override public void onExpand(String url)                              {}
            @Override public void onResize(int w, int h, int x, int y, boolean ao) {}
            @Override public void onOpen(String url)                                {}
            @Override public void onLog(String m, String l)                         {}
            @Override public void onStateChange(MRAIDBridge.MRAIDState s)           {}
        });

        webView.addJavascriptInterface(mraidBridge, "ApexMRAID");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                if (activeListener != null) activeListener.onInterstitialClicked();
                return false;
            }
            @Override
            public void onPageFinished(WebView v, String url) {
                mraidBridge.notifyReady(webView, "interstitial");
            }
        });

        String html = "<!DOCTYPE html><html><head>" +
            "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
            "<style>html,body{margin:0;padding:0;overflow:hidden;}</style>" +
            "<script>" + MRAIDBridge.getMRAIDScript() + "</script>" +
            "</head><body>" + adData.adMarkup + "</body></html>";
        webView.loadDataWithBaseURL("https://apexads.sdk", html, "text/html", "UTF-8", null);

        // Countdown badge — top-right, circular
        tvCountdown = new TextView(this);
        tvCountdown.setTextColor(Color.WHITE);
        tvCountdown.setTextSize(16f);
        tvCountdown.setTypeface(tvCountdown.getTypeface(), Typeface.BOLD);
        tvCountdown.setGravity(Gravity.CENTER);
        tvCountdown.setText(String.valueOf(CLOSE_DELAY_SECONDS));
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(0xCC000000);
        tvCountdown.setBackground(circle);
        FrameLayout.LayoutParams cdParams = new FrameLayout.LayoutParams(dp(40), dp(40));
        cdParams.gravity = Gravity.TOP | Gravity.END;
        cdParams.setMargins(0, dp(12), dp(12), 0);
        root.addView(tvCountdown, cdParams);

        // Close button — same corner, hidden until countdown ends
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

        // Wallet CTA panel — attached when sdk-wallet is installed and ext.wallet present
        if (adData.walletExtJson != null && ServiceLocator.isRegistered(WalletDelegate.class)) {
            WalletDelegate delegate = ServiceLocator.get(WalletDelegate.class);
            delegate.attachToInterstitial(this, root, adData.walletExtJson,
                    new WalletDelegate.WalletEventCallback() {
                        @Override public void onPassSaved() {
                            if (activeListener != null) activeListener.onWalletPassSaved();
                        }
                        @Override public void onPassCancelled() {
                            if (activeListener != null) activeListener.onWalletPassCancelled();
                        }
                        @Override public void onPassFailed() {
                            if (activeListener != null) activeListener.onWalletPassFailed();
                        }
                    });
        }

        mainHandler.post(closeTickRunnable);

        if (activeListener != null) activeListener.onInterstitialShown();
        AdLog.d("InterstitialActivity: showing ad cpm=$%.2f closeDelay=%ds wallet=%s",
                adData.cpm, CLOSE_DELAY_SECONDS, adData.walletExtJson != null ? "yes" : "no");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (ServiceLocator.isRegistered(WalletDelegate.class)) {
            ServiceLocator.get(WalletDelegate.class).handleActivityResult(requestCode, resultCode);
        }
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacks(closeTickRunnable);
        if (webView != null) webView.destroy();
        if (activeListener != null) activeListener.onInterstitialClosed();
        activeListener = null;
        pendingAdData = null;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (btnClose != null && btnClose.getVisibility() == View.VISIBLE) {
            finish();
        }
        // Swallow back-press while countdown is running
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void showCloseButton() {
        tvCountdown.setVisibility(View.GONE);
        btnClose.setVisibility(View.VISIBLE);
    }

    private void window() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // ── Static launch ─────────────────────────────────────────────────────────

    static void launch(@NonNull Context context,
                       @NonNull AdData adData,
                       @Nullable InterstitialAdListener listener) {
        pendingAdData = adData;
        activeListener = listener;
        Intent intent = new Intent(context, InterstitialActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
