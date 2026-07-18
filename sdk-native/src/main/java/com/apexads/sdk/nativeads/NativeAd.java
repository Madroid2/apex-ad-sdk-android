package com.apexads.sdk.nativeads;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.IntentAction;
import com.apexads.sdk.core.models.IntentContext;
import com.apexads.sdk.core.models.NativeAdPayload;
import com.apexads.sdk.core.di.WalletDelegate;
import com.apexads.sdk.core.domain.repository.AdRepository;
import com.apexads.sdk.core.presentation.mvvm.AdState;
import com.apexads.sdk.core.presentation.mvvm.AdStateObserver;
import com.apexads.sdk.core.presentation.mvvm.AdViewModelListener;
import com.apexads.sdk.core.data.repository.OpenRTBAdRepository;
import com.apexads.sdk.core.request.OpenRTBRequestBuilder;
import com.apexads.sdk.core.utils.AdLog;
import com.apexads.sdk.internal.ApexSdkRuntime;
import com.apexads.sdk.internal.ApexFeatureAccess;

import java.util.concurrent.atomic.AtomicBoolean;

public final class NativeAd {

    private final NativeAdViewModel viewModel;
    @Nullable private final NativeAdListener listener;
    private final AtomicBoolean actionRendered = new AtomicBoolean(false);

    private NativeAd(Builder builder) {
        OpenRTBRequestBuilder requestBuilder = new OpenRTBRequestBuilder(
                ApexSdkRuntime.getDeviceInfoProvider(),
                ApexSdkRuntime.getConsentManager())
                .intentContext(builder.intentContext)
                .renderSurface(builder.renderSurface);
        AdRepository repository = new OpenRTBAdRepository(
                ApexSdkRuntime.getNetworkClient(),
                requestBuilder);

        viewModel = new NativeAdViewModel(
                repository,
                new AdCache(),
                builder.placementId != null ? builder.placementId : "",
                new NativeAdParser());

        this.listener = builder.listener;

        viewModel.setViewListener(new AdViewModelListener() {
            @Override
            public void onAdLoaded(@NonNull AdData adData) {
                if (listener != null) listener.onNativeAdLoaded(NativeAd.this);
            }

            @Override
            public void onAdFailed(@NonNull AdError error) {
                if (listener != null) listener.onNativeAdFailed(error);
            }

            @Override
            public void onAdExpired() {
                if (listener != null)
                    listener.onNativeAdFailed(new AdError.NoFill("Cached ad expired"));
            }
        });
    }

    public void load() {
        actionRendered.set(false);
        viewModel.load();
    }

    public void bindTo(@NonNull NativeAdView view) {
        NativeAdPayload payload = viewModel.getNativePayload();
        if (payload == null) {
            AdLog.w("NativeAd: bindTo() called before ad was loaded");
            return;
        }
        IntentAction action = getIntentAction();
        view.bind(
                payload,
                ApexSdkRuntime.getTrackingClient(),
                action,
                action != null ? () -> performAction(view.getContext()) : null,
                action != null ? this::recordActionRendered : null);
    }

    /**
     * Triggers the ad's click-through for a custom-rendered native layout
     * (e.g. Jetpack Compose), where the publisher draws the assets themselves
     * instead of binding a {@link NativeAdView}. Opens the click URL in the
     * browser and notifies {@code onNativeAdClicked}. Wire this to the
     * onClick of your CTA (and/or the whole ad card).
     *
     * @return true if a click-through was opened.
     */
    public boolean handleClick(@NonNull Context context) {
        NativeAdPayload payload = viewModel.getNativePayload();
        if (payload == null || payload.clickUrl == null) {
            AdLog.w("NativeAd: handleClick() before load, or ad has no click URL");
            return false;
        }
        // Deep-link aware: web URLs open as before; app deep links (custom
        // scheme / market://) launch the app, with link.fallback as the web
        // fallback and client-side click-tracker firing (see NativeAdClicks).
        boolean opened = NativeAdClicks.open(
                context, payload, ApexSdkRuntime.getTrackingClient(), "NativeAd");
        if (opened && listener != null) {
            listener.onNativeAdClicked();
        }
        return opened;
    }

    /**
     * Executes the bid's optional action. If the action module is unavailable, the standard
     * Native 1.2 click-through remains the fallback.
     */
    public boolean performAction(@NonNull Context context) {
        IntentAction action = getIntentAction();
        AdData adData = viewModel.getAdData();
        if (action == null || adData == null) return handleClick(context);

        if (action.type == IntentContext.ActionType.OPEN_DEEPLINK) {
            fireTracking(action.startedTrackingUrl);
            return handleClick(context);
        }

        WalletDelegate delegate = ApexFeatureAccess.getFeature(WalletDelegate.class);
        if (delegate == null || adData.walletExtJson == null || !delegate.isAvailable(context)) {
            return handleClick(context);
        }
        fireTracking(action.startedTrackingUrl);
        boolean started = delegate.performAction(
                context,
                adData.walletExtJson,
                new WalletDelegate.WalletEventCallback() {
                    @Override public void onPassSaved() {
                        if (listener != null) listener.onNativeAdActionCompleted();
                    }

                    @Override public void onPassCancelled() {
                        fireTracking(action.cancelledTrackingUrl);
                        if (listener != null) listener.onNativeAdActionCancelled();
                    }

                    @Override public void onPassFailed() {
                        fireTracking(action.failedTrackingUrl);
                        if (listener != null) listener.onNativeAdActionFailed();
                    }
                });
        return started || handleClick(context);
    }

    /** Records the action CTA as rendered once per loaded card. */
    public void recordActionRendered() {
        IntentAction action = getIntentAction();
        if (action != null && actionRendered.compareAndSet(false, true)) {
            fireTracking(action.renderedTrackingUrl);
        }
    }

    private void fireTracking(@Nullable String url) {
        if (url == null) return;
        com.apexads.sdk.core.network.SdkExecutors.IO.execute(
                () -> ApexSdkRuntime.getTrackingClient().fireTrackingUrl(url));
    }

    public boolean isReady() {
        return viewModel.isReady();
    }

    @NonNull
    public AdState getState() {
        return viewModel.getState();
    }

    public void addStateObserver(@NonNull AdStateObserver observer) {
        viewModel.getStateObservable().addObserver(observer);
    }

    public void removeStateObserver(@NonNull AdStateObserver observer) {
        viewModel.getStateObservable().removeObserver(observer);
    }

    public void destroy() {
        viewModel.destroy();
    }

    @Nullable public String getTitle() { NativeAdPayload p = viewModel.getNativePayload(); return p != null ? p.title : null; }
    @Nullable public String getDescription() { NativeAdPayload p = viewModel.getNativePayload(); return p != null ? p.description : null; }
    @Nullable public String getCtaText() { NativeAdPayload p = viewModel.getNativePayload(); return p != null ? p.ctaText : null; }
    @Nullable public String getIconUrl() { NativeAdPayload p = viewModel.getNativePayload(); return p != null ? p.iconUrl : null; }
    @Nullable public String getImageUrl() { NativeAdPayload p = viewModel.getNativePayload(); return p != null ? p.imageUrl : null; }
    @Nullable public String getAdvertiserName() { NativeAdPayload p = viewModel.getNativePayload(); return p != null ? p.advertiserName : null; }
    @Nullable public IntentAction getIntentAction() {
        AdData data = viewModel.getAdData();
        return data != null ? IntentAction.fromJson(data.actionExtJson) : null;
    }
    public boolean hasIntentAction() { return getIntentAction() != null; }
    @Nullable public String getIntentLabel() { IntentAction a = getIntentAction(); return a != null ? a.intentLabel : null; }
    @Nullable public String getActionCtaText() { IntentAction a = getIntentAction(); return a != null ? a.ctaText : null; }
    @Nullable public String getDisclosureText() { IntentAction a = getIntentAction(); return a != null ? a.disclosure : null; }
    @Nullable public String getActionBadgeText() { IntentAction a = getIntentAction(); return a != null ? a.badgeText : null; }

    public static final class Builder {
        private final String placementId;
        @Nullable private NativeAdListener listener;
        @Nullable private IntentContext intentContext;
        @Nullable private String renderSurface;

        public Builder(@Nullable String placementId) { this.placementId = placementId; }

        public Builder listener(@NonNull NativeAdListener l) { listener = l; return this; }

        public Builder intentContext(@Nullable IntentContext context) {
            intentContext = context;
            return this;
        }

        /**
         * Declares the in-app surface this placement renders on, carried as
         * {@code imp.ext.apex.surface}. Used by Apex surface modules (e.g.
         * {@code sdk-conversational}); most publishers never call this directly.
         */
        public Builder renderSurface(@Nullable String surface) {
            renderSurface = surface;
            return this;
        }

        @NonNull
        public NativeAd build() {
            if (!ApexSdkRuntime.isInitialized()) {
                throw new IllegalStateException(
                        "Call ApexAds.init() before creating ad instances.");
            }
            return new NativeAd(this);
        }
    }
}
