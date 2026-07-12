package com.apexads.sdk.measurement;

import android.view.View;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.di.MeasurementDelegate;
import com.apexads.sdk.core.utils.AdLog;

/**
 * OM SDK (OMID) implementation of {@link MeasurementDelegate}.
 *
 * <p><b>Scaffold status:</b> the session plumbing, bid-request signaling, and render-path
 * wiring are complete, but the IAB Tech Lab OM SDK AAR is not yet bundled — it is only
 * distributed through the OM SDK portal after partner registration. Until the AAR lands
 * and {@code OMID_ACTIVE} is flipped, {@link #isReady()} stays {@code false}, which keeps
 * {@code api=7} / {@code source.ext.omidpn} out of bid requests: the SDK must never
 * declare OMID to exchanges while measurement cannot actually run.</p>
 *
 * <p>Remaining integration steps, in order:</p>
 * <ol>
 *   <li>Register the partner name with IAB Tech Lab; receive the OM SDK AAR + namespace.</li>
 *   <li>Bundle the AAR (see {@code sdk-measurement/build.gradle.kts}) and set
 *       {@code OMID_ACTIVE = true}.</li>
 *   <li>{@code Omid.activate(context)} at install; {@code Partner.createPartner(PARTNER_NAME, version)}.</li>
 *   <li>{@link #enrichHtml}: {@code ScriptInjector.injectScriptContentIntoHtml(omidJs, html)}.</li>
 *   <li>{@link #startDisplaySession}: {@code AdSessionConfiguration.createAdSessionConfiguration(
 *       CreativeType.HTML_DISPLAY, ImpressionType.VIEWABLE, Owner.NATIVE, Owner.NONE, false)}
 *       + {@code AdSessionContext.createHtmlAdSessionContext(partner, webView, null, null)};
 *       {@code adSession.registerAdView(adView)}, {@code adSession.start()},
 *       {@code AdEvents.createAdEvents(adSession).loaded()}.</li>
 *   <li>{@code AdSession.impressionOccurred()} via {@code AdEvents}; {@code adSession.finish()}.</li>
 *   <li>Submit for IAB compliance certification; then video sessions (VAST
 *       {@code <AdVerifications>}) ride the same delegate.</li>
 * </ol>
 */
public final class OmidMeasurementDelegate implements MeasurementDelegate {

    /** Flip only after the IAB OM SDK AAR is bundled and partner registration is complete. */
    static final boolean OMID_ACTIVE = false;

    static final String PARTNER_NAME = "Apexads";
    static final String OM_SDK_VERSION = "1.5.0";

    @Override
    public boolean isReady() {
        return OMID_ACTIVE;
    }

    @NonNull
    @Override
    public String partnerName() {
        return PARTNER_NAME;
    }

    @NonNull
    @Override
    public String partnerVersion() {
        return OM_SDK_VERSION;
    }

    @NonNull
    @Override
    public String enrichHtml(@NonNull String html) {
        if (!isReady()) return html;
        // ScriptInjector.injectScriptContentIntoHtml(omidServiceJs, html)
        return html;
    }

    @NonNull
    @Override
    public AdSession startDisplaySession(@NonNull View adView, @NonNull WebView webView) {
        if (!isReady()) {
            AdLog.d("OmidMeasurementDelegate: OM SDK not active — no-op session");
            return AdSession.NO_OP;
        }
        // createAdSessionConfiguration + createHtmlAdSessionContext + registerAdView + start
        return AdSession.NO_OP;
    }
}
