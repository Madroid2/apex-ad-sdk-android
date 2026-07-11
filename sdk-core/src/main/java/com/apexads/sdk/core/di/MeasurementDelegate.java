package com.apexads.sdk.core.di;

import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;

/**
 * Optional feature contract for buyer-verifiable viewability measurement
 * (IAB Open Measurement / OMID). Installed by the {@code sdk-measurement}
 * module; {@code sdk-core} only ever sees this interface, so the OM SDK
 * dependency stays scoped to the feature module — same pattern as
 * {@link WalletDelegate}.
 *
 * <p>The in-house {@code ImpressionTracker} remains the SDK's billing trigger;
 * this contract exists so DSP-side measurement (DV360 Active View, IAS, DV,
 * Moat) can observe the same ad session through OMID.</p>
 */
public interface MeasurementDelegate extends SdkFeature {

    /**
     * True only when a certified OM SDK build is active for this partner.
     * Gates all OMID bid-request signaling ({@code api=7},
     * {@code source.ext.omidpn/omidpv}) — the SDK must never declare OMID
     * support to exchanges while measurement cannot actually run.
     */
    boolean isReady();

    /** OMID partner name as registered with IAB Tech Lab ({@code source.ext.omidpn}). */
    @NonNull
    String partnerName();

    /** OM SDK version in use ({@code source.ext.omidpv}). */
    @NonNull
    String partnerVersion();

    /**
     * Injects the OMID JS service script into creative HTML before it is loaded
     * into the ad WebView. Returns the input unchanged when not ready.
     */
    @NonNull
    String enrichHtml(@NonNull String html);

    /**
     * Starts an OMID HTML display session bound to the rendered ad view.
     * Must return a non-null session; implementations return a no-op session
     * when not ready so call sites need no branching.
     */
    @NonNull
    AdSession startDisplaySession(@NonNull View adView, @NonNull WebView webView);

    /** Handle for one creative's measurement session. Implementations must be idempotent. */
    interface AdSession {
        /** Signals the OMID impression event; call at the MRC-viewable impression. */
        void impressionOccurred();

        /** Ends the session; call on ad replacement or view teardown. */
        void finish();

        AdSession NO_OP = new AdSession() {
            @Override public void impressionOccurred() {}
            @Override public void finish() {}
        };
    }
}
