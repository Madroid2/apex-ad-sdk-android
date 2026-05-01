package com.apexads.sdk.interstitial;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.app.Activity;

import com.apexads.sdk.banner.mraid.MRAIDBridge;
import com.apexads.sdk.core.models.AdData;

import com.apexads.sdk.core.utils.AdLog;

/**
 * Fullscreen Activity that hosts the interstitial WebView.
 *
 * Launched by {@link InterstitialAd#show(Context)}. Uses a static slot to hand
 * off {@link AdData} and {@link InterstitialAdListener} without Parcelable overhead —
 * safe because this Activity is always SDK-internal and short-lived.
 */
public final class InterstitialActivity extends Activity {

    // Static slots — cleared immediately in onDestroy
    private static volatile AdData         pendingAdData;
    private static volatile InterstitialAdListener activeListener;

    private WebView webView;
    private MRAIDBridge mraidBridge;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AdData adData = pendingAdData;
        if (adData == null) { finish(); return; }

        window();

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        setContentView(webView);

        mraidBridge = new MRAIDBridge(new MRAIDBridge.MRAIDListener() {
            @Override public void onClose()                          { finish(); }
            @Override public void onExpand(String url)               {}
            @Override public void onResize(int w, int h, int x, int y, boolean ao) {}
            @Override public void onOpen(String url)                 {}
            @Override public void onLog(String m, String l)          {}
            @Override public void onStateChange(MRAIDBridge.MRAIDState s) {}
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

        if (activeListener != null) activeListener.onInterstitialShown();
        AdLog.d("InterstitialActivity: showing ad cpm=$%.2f", adData.cpm);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) webView.destroy();
        if (activeListener != null) activeListener.onInterstitialClosed();
        activeListener = null;
        pendingAdData  = null;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void window() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    static void launch(@NonNull Context context,
                       @NonNull AdData adData,
                       @Nullable InterstitialAdListener listener) {
        pendingAdData  = adData;
        activeListener = listener;
        Intent intent = new Intent(context, InterstitialActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
