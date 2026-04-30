package com.apexads.sdk.banner;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.ApexAds;
import com.apexads.sdk.banner.mraid.MRAIDBridge;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.tracking.ImpressionTracker;

import com.apexads.sdk.core.utils.AdLog;

/**
 * Drop-in banner ad container.
 *
 * Add to layout XML, then call {@link BannerAd#show(BannerAdView)} after load:
 * <pre>{@code
 * <com.apexads.sdk.banner.BannerAdView
 *     android:id="@+id/banner_ad"
 *     android:layout_width="320dp"
 *     android:layout_height="50dp" />
 * }</pre>
 */
public final class BannerAdView extends FrameLayout {

    private final WebView webView;
    private MRAIDBridge mraidBridge;
    private ImpressionTracker impressionTracker;
    @Nullable private BannerAdListener listener;

    public BannerAdView(@NonNull Context context) {
        this(context, null);
    }

    public BannerAdView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BannerAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        webView = new WebView(context);
        setupWebView();
        addView(webView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        mraidBridge = new MRAIDBridge(new MRAIDBridge.MRAIDListener() {
            @Override public void onClose()                { notifyListener(l -> l.onAdClosed()); }
            @Override public void onExpand(String url)     { notifyListener(l -> l.onAdExpanded()); }
            @Override public void onResize(int w, int h, int ox, int oy, boolean ao) {}
            @Override public void onOpen(String url)       { openUrl(url); }
            @Override public void onLog(String m, String lvl) {}
            @Override public void onStateChange(MRAIDBridge.MRAIDState state) {}
        });

        webView.addJavascriptInterface(mraidBridge, "ApexMRAID");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                openUrl(request.getUrl().toString());
                notifyListener(l -> l.onAdClicked());
                return true;
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                mraidBridge.notifyReady(webView, "inline");
                AdLog.d("BannerAdView: MRAID ready fired");
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
    }

    public void setListener(@Nullable BannerAdListener listener) {
        this.listener = listener;
    }

    /** Called by {@link BannerAd#show(BannerAdView)} — not part of the public publisher API. */
    void render(@NonNull AdData adData) {
        impressionTracker = new ImpressionTracker(ApexAds.getNetworkClient());

        String html = "<!DOCTYPE html><html><head>" +
            "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1\">" +
            "<style>html,body{margin:0;padding:0;overflow:hidden;}</style>" +
            "<script>" + MRAIDBridge.getMRAIDScript() + "</script>" +
            "</head><body>" + adData.adMarkup + "</body></html>";

        webView.loadDataWithBaseURL("https://apexads.sdk", html, "text/html", "UTF-8", null);
        impressionTracker.attach(this, adData);
        AdLog.d("BannerAdView: rendering ad cpm=$%.2f", adData.cpm);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        destroy();
    }

    public void destroy() {
        webView.stopLoading();
        webView.clearHistory();
        webView.removeAllViews();
        webView.destroy();
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
        } catch (Exception e) {
            AdLog.w(e, "BannerAdView: could not open URL: %s", url);
        }
    }

    private interface ListenerAction {
        void run(BannerAdListener l);
    }

    private void notifyListener(ListenerAction action) {
        if (listener != null) action.run(listener);
    }
}
