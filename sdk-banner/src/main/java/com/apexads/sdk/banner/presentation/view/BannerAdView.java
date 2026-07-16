package com.apexads.sdk.banner.presentation.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.BuildConfig;
import com.apexads.sdk.banner.BannerAd;
import com.apexads.sdk.banner.BannerAdListener;
import com.apexads.sdk.banner.BannerAdViewModel;
import com.apexads.sdk.banner.presentation.view.mraid.MRAIDBridge;
import com.apexads.sdk.core.di.MeasurementDelegate;
import com.apexads.sdk.core.di.WalletDelegate;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.presentation.mvvm.AdState;
import com.apexads.sdk.core.presentation.mvvm.AdStateObserver;
import com.apexads.sdk.core.tracking.ImpressionTracker;
import com.apexads.sdk.core.utils.AdNavigationGuard;
import com.apexads.sdk.core.utils.AdUrlHandler;
import com.apexads.sdk.core.utils.AdViewLifecycle;
import com.apexads.sdk.core.utils.AdLog;
import com.apexads.sdk.internal.ApexFeatureAccess;
import com.apexads.sdk.internal.ApexSdkRuntime;

public class BannerAdView extends FrameLayout {

    // Synthetic base origin for the MRAID document. Debug uses http so it is
    // same-scheme with the local demand platform's http creative assets (no
    // mixed-content block); release uses https to match production assets.
    private static final String MRAID_BASE_URL =
            BuildConfig.DEBUG ? "http://apexads.sdk" : "https://apexads.sdk";

    private final WebView webView;
    private final AdNavigationGuard navigationGuard = new AdNavigationGuard("BannerAdView");
    private MRAIDBridge mraidBridge;
    private ImpressionTracker impressionTracker;

    @Nullable private BannerAdListener listener;
    @Nullable private BannerAdViewModel boundViewModel;
    @Nullable private AdData renderedAdData;
    @Nullable private MeasurementDelegate.AdSession measurementSession;

    private final AdStateObserver stateObserver;
    private boolean destroyed;

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
                if (!destroyed) webView.loadUrl("about:blank");
            }
        };
        setupWebView();
        addView(webView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void bind(@NonNull BannerAdViewModel viewModel, @Nullable BannerAdListener adListener) {
        if (destroyed) {
            AdLog.w("BannerAdView: bind() ignored after destroy()");
            return;
        }

        if (boundViewModel != null) {
            boundViewModel.getStateObservable().removeObserver(stateObserver);
        }
        boundViewModel = viewModel;
        listener = adListener;
        viewModel.getStateObservable().addObserver(stateObserver);

        // Re-render retained creative after a configuration change (e.g. rotation).
        // The ViewModel stays DISPLAYED across rotation; the view is new and needs re-populating.
        if (viewModel.getState() == AdState.DISPLAYED) {
            AdData retained = viewModel.getAdData();
            if (retained != null && !retained.isExpired()) {
                AdLog.d("BannerAdView: re-rendering retained creative after config change");
                renderInternal(retained, viewModel.hasImpressionFired());
            }
        }
    }

    public void render(@NonNull AdData adData) {
        renderInternal(adData, false);
    }

    private void renderInternal(@NonNull AdData adData, boolean skipImpression) {
        if (destroyed) {
            AdLog.w("BannerAdView: render() ignored after destroy()");
            return;
        }

        if (impressionTracker != null) {
            impressionTracker.detach();
            impressionTracker = null;
        }
        renderedAdData = adData;
        navigationGuard.reset(adData);

        String html = "<!DOCTYPE html><html><head>"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1\">"
                + "<style>html,body{margin:0;padding:0;overflow:hidden;}</style>"
                + "<script>" + MRAIDBridge.getMRAIDScript() + "</script>"
                + "</head><body>" + adData.adMarkup + "</body></html>";

        // OMID: one measurement session per rendered creative. The delegate injects
        // the OMID JS service into the document so DSP-side verification scripts
        // (DV360 Active View, IAS, DV) can observe this WebView.
        if (measurementSession != null) {
            measurementSession.finish();
            measurementSession = null;
        }
        MeasurementDelegate measurement = ApexFeatureAccess.getFeature(MeasurementDelegate.class);
        if (measurement != null) {
            html = measurement.enrichHtml(html);
        }

        // Base origin scheme must match the creative asset scheme or the assets
        // are "mixed content" and Chromium blocks them (even with ALWAYS_ALLOW,
        // which is unreliable for loadDataWithBaseURL). Debug creatives load http
        // assets from the local demand platform; production loads https. Matching
        // the scheme eliminates mixed content entirely.
        webView.loadDataWithBaseURL(MRAID_BASE_URL, html, "text/html", "UTF-8", null);

        if (measurement != null) {
            measurementSession = measurement.startDisplaySession(this, webView);
        }

        if (!skipImpression) {
            impressionTracker = new ImpressionTracker(ApexSdkRuntime.getTrackingClient());
            impressionTracker.attach(this, adData, this::onViewableImpression);
        }

        WalletDelegate delegate = ApexFeatureAccess.getFeature(WalletDelegate.class);
        if (adData.walletExtJson != null
                && adData.width >= 300 && adData.height >= 250
                && delegate != null) {
            delegate.attachToBanner(getContext(), this, adData,
                    new WalletDelegate.WalletEventCallback() {
                        @Override public void onPassSaved() { notifyListener(BannerAdListener::onWalletPassSaved); }
                        @Override public void onPassCancelled() { notifyListener(BannerAdListener::onWalletPassCancelled); }
                        @Override public void onPassFailed() { notifyListener(BannerAdListener::onWalletPassFailed); }
                    });
        }

        AdLog.d("BannerAdView: rendering cpm=$%.2f wallet=%s skip_impression=%b",
                adData.cpm, adData.walletExtJson != null ? "yes" : "no", skipImpression);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        navigationGuard.recordTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        resumeOffscreenWork();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        pauseOffscreenWork();

        if (AdViewLifecycle.isTerminalDetach(this)) {
            destroy();
        } else {
            AdLog.d("BannerAdView: transient detach — retaining WebView state");
        }
    }

    /**
     * Tears down this view's resources (WebView, impression tracker, listener).  The bound
     * ViewModel is intentionally NOT destroyed here — it may survive a configuration change
     * to allow re-rendering the creative into a new view without a fresh network request.
     * The publisher is responsible for calling {@link BannerAd#destroy()} when the placement
     * is permanently removed (e.g. in {@code ViewModel.onCleared()}).
     */
    public void destroy() {
        // WebView throws if touched again after destroy() — make re-entry a no-op.
        if (destroyed) return;
        destroyed = true;

        if (boundViewModel != null) {
            boundViewModel.getStateObservable().removeObserver(stateObserver);
            boundViewModel = null;
        }

        if (impressionTracker != null) {
            impressionTracker.detach();
            impressionTracker = null;
        }
        renderedAdData = null;

        if (measurementSession != null) {
            measurementSession.finish();
            measurementSession = null;
        }

        listener = null;

        // Tear down the WebView, including the MRAID JS bridge.
        webView.stopLoading();
        webView.removeJavascriptInterface("ApexMRAID");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(null);
        webView.loadUrl("about:blank");
        webView.clearHistory();
        webView.removeAllViews();
        webView.destroy();
        mraidBridge = null;
    }

    private void pauseOffscreenWork() {
        if (destroyed) return;

        if (impressionTracker != null) {
            impressionTracker.detach();
        }
        notifyMraidViewable(false);
        try {
            webView.onPause();
        } catch (Exception e) {
            AdLog.w(e, "BannerAdView: WebView onPause failed");
        }
    }

    private void resumeOffscreenWork() {
        if (destroyed) return;

        try {
            webView.onResume();
        } catch (Exception e) {
            AdLog.w(e, "BannerAdView: WebView onResume failed");
        }

        if (impressionTracker != null && renderedAdData != null && !renderedAdData.isExpired()) {
            impressionTracker.attach(this, renderedAdData, this::onViewableImpression);
        }
        notifyMraidViewable(true);
    }

    /** Single MRC-viewable impression sink: ViewModel state + OMID measurement event. */
    private void onViewableImpression() {
        BannerAdViewModel vm = boundViewModel;
        if (vm != null) vm.markImpressionFired();
        MeasurementDelegate.AdSession session = measurementSession;
        if (session != null) session.impressionOccurred();
    }

    private void notifyMraidViewable(boolean viewable) {
        if (mraidBridge == null || destroyed) return;
        try {
            mraidBridge.notifyViewableChange(webView, viewable);
        } catch (Exception e) {
            AdLog.w(e, "BannerAdView: MRAID viewability update failed");
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        // The ad document is loaded with an https base origin (apexads.sdk), so
        // http creative assets are "mixed content". In production, creatives are
        // served over https and COMPATIBILITY_MODE is correct. In debug the local
        // demand platform serves assets over plain http on the LAN, so allow
        // mixed content there or product images render broken.
        settings.setMixedContentMode(
                BuildConfig.DEBUG
                        ? WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        : WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        mraidBridge = new MRAIDBridge(new MRAIDBridge.MRAIDListener() {
            @Override public void onClose() { notifyListener(BannerAdListener::onAdClosed); }
            @Override public void onExpand(String url) { notifyListener(BannerAdListener::onAdExpanded); }
            @Override public void onResize(int w, int h, int ox, int oy, boolean ao) {}
            @Override public void onOpen(String url) { openUrl(url, "mraid.open"); }
            @Override public void onLog(String m, String lvl) {}
            @Override public void onStateChange(MRAIDBridge.MRAIDState state) {}
            @Override public void onNavigationAttempt(String type, String url) {
                navigationGuard.reportJsNavigationAttempt(type, url);
            }
        });

        webView.addJavascriptInterface(mraidBridge, "ApexMRAID");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || !request.isForMainFrame()) {
                    return true;
                }
                openUrl(request.getUrl().toString(), request, "webview-nav");
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                openUrl(url, "webview-nav-legacy");
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

    private void openUrl(String url, String trigger) {
        AdNavigationGuard.Decision decision =
                navigationGuard.evaluateNavigation(url, false, false, trigger);
        openUrlIfAllowed(decision);
    }

    private void openUrl(String url, WebResourceRequest request, String trigger) {
        AdNavigationGuard.Decision decision =
                navigationGuard.evaluateNavigation(url, request, trigger);
        openUrlIfAllowed(decision);
    }

    private void openUrlIfAllowed(AdNavigationGuard.Decision decision) {
        if (!decision.allowed) return;
        boolean opened;
        if (decision.deeplink != null) {
            // App deep link from the creative — launch it (market:// falls back
            // to the Play Store web page when no store app is present).
            opened = AdUrlHandler.openClickThrough(
                    getContext(), decision.deeplink, null, "BannerAdView") != AdUrlHandler.OPEN_FAILED;
        } else {
            opened = decision.safeUrl != null
                    && AdUrlHandler.openValidatedExternalUrl(getContext(), decision.safeUrl, "BannerAdView");
        }
        if (opened) {
            notifyListener(BannerAdListener::onAdClicked);
        }
    }

    private void notifyListener(ListenerAction action) {
        if (listener != null) action.run(listener);
    }

    private interface ListenerAction {
        void run(BannerAdListener l);
    }
}
