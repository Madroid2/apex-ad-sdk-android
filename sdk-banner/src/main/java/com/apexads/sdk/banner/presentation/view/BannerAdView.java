package com.apexads.sdk.banner.presentation.view;

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
import com.apexads.sdk.banner.BannerAd;
import com.apexads.sdk.banner.BannerAdListener;
import com.apexads.sdk.banner.BannerAdViewModel;
import com.apexads.sdk.banner.presentation.view.mraid.MRAIDBridge;
import com.apexads.sdk.core.di.ServiceLocator;
import com.apexads.sdk.core.di.WalletDelegate;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.presentation.mvvm.AdState;
import com.apexads.sdk.core.presentation.mvvm.AdStateObserver;
import com.apexads.sdk.core.tracking.ImpressionTracker;
import com.apexads.sdk.core.utils.AdLog;

public class BannerAdView extends FrameLayout {

    private final WebView webView;
    private MRAIDBridge mraidBridge;
    private ImpressionTracker impressionTracker;

    @Nullable private BannerAdListener listener;
    @Nullable private BannerAdViewModel boundViewModel;

    private final AdStateObserver stateObserver;

    public BannerAdView(@NonNull Context context) {
        this(context, null);
    }

    public BannerAdView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BannerAdView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        webView = new WebView(context);

        stateObserver = state -> {
            if (state == AdState.EXPIRED) {
                AdLog.d("BannerAdView: ad expired — clearing WebView");
                webView.loadUrl("about:blank");
            }
        };
        setupWebView();
        addView(webView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void bind(@NonNull BannerAdViewModel viewModel, @Nullable BannerAdListener adListener) {

        if (boundViewModel != null) {
            boundViewModel.getStateObservable().removeObserver(stateObserver);
        }
        boundViewModel = viewModel;
        listener = adListener;
        viewModel.getStateObservable().addObserver(stateObserver);
    }

    public void render(@NonNull AdData adData) {
        impressionTracker = new ImpressionTracker(ApexAds.getNetworkClient());

        String html = "<!DOCTYPE html><html><head>"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1\">"
                + "<style>html,body{margin:0;padding:0;overflow:hidden;}</style>"
                + "<script>" + MRAIDBridge.getMRAIDScript() + "</script>"
                + "</head><body>" + adData.adMarkup + "</body></html>";

        webView.loadDataWithBaseURL("https://apexads.sdk", html, "text/html", "UTF-8", null);
        impressionTracker.attach(this, adData);

        if (adData.walletExtJson != null
                && adData.width >= 300 && adData.height >= 250
                && ServiceLocator.isRegistered(WalletDelegate.class)) {
            WalletDelegate delegate = ServiceLocator.get(WalletDelegate.class);
            delegate.attachToBanner(getContext(), this, adData,
                    new WalletDelegate.WalletEventCallback() {
                        @Override public void onPassSaved() { notifyListener(BannerAdListener::onWalletPassSaved); }
                        @Override public void onPassCancelled() { notifyListener(BannerAdListener::onWalletPassCancelled); }
                        @Override public void onPassFailed() { notifyListener(BannerAdListener::onWalletPassFailed); }
                    });
        }

        AdLog.d("BannerAdView: rendering cpm=$%.2f wallet=%s",
                adData.cpm, adData.walletExtJson != null ? "yes" : "no");
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (boundViewModel != null) {
            boundViewModel.getStateObservable().removeObserver(stateObserver);
            boundViewModel = null;
        }
        destroy();
    }

    public void destroy() {
        webView.stopLoading();
        webView.clearHistory();
        webView.removeAllViews();
        webView.destroy();
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
            @Override public void onClose() { notifyListener(BannerAdListener::onAdClosed); }
            @Override public void onExpand(String url) { notifyListener(BannerAdListener::onAdExpanded); }
            @Override public void onResize(int w, int h, int ox, int oy, boolean ao) {}
            @Override public void onOpen(String url) { openUrl(url); }
            @Override public void onLog(String m, String lvl) {}
            @Override public void onStateChange(MRAIDBridge.MRAIDState state) {}
        });

        webView.addJavascriptInterface(mraidBridge, "ApexMRAID");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                openUrl(request.getUrl().toString());
                notifyListener(BannerAdListener::onAdClicked);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                mraidBridge.notifyReady(webView, "inline");
                notifyListener(BannerAdListener::onAdImpression);
                AdLog.d("BannerAdView: MRAID ready fired");
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
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

    private void notifyListener(ListenerAction action) {
        if (listener != null) action.run(listener);
    }

    private interface ListenerAction {
        void run(BannerAdListener l);
    }
}
