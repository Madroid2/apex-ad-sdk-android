package com.apexads.sdk.measurement;

import static com.google.common.truth.Truth.assertThat;

import com.apexads.sdk.core.di.MeasurementDelegate;

import org.junit.Test;

public class OmidMeasurementDelegateTest {

    private final OmidMeasurementDelegate delegate = new OmidMeasurementDelegate();

    @Test
    public void isReady_falseUntilOmSdkBundled() {
        // Guards against accidentally signaling api=7 / omidpn to exchanges
        // before the IAB OM SDK AAR is bundled and certified.
        assertThat(delegate.isReady()).isFalse();
    }

    @Test
    public void enrichHtml_passthroughWhileNotReady() {
        String html = "<html><body>ad</body></html>";
        assertThat(delegate.enrichHtml(html)).isSameInstanceAs(html);
    }

    @Test
    public void partnerIdentity_isUnregisteredUntilIabNamespaceIsConfigured() {
        assertThat(delegate.partnerName()).isEmpty();
        assertThat(delegate.partnerVersion()).isNotEmpty();
        assertThat(delegate.partnerVersion()).isEqualTo(com.apexads.sdk.BuildConfig.SDK_VERSION);
    }

    @Test
    public void targetOmSdkVersion_tracksCurrentIabRelease() {
        assertThat(OmidMeasurementDelegate.TARGET_OM_SDK_VERSION).isEqualTo("1.6.0");
    }

    @Test
    public void noOpSession_isSafeToUse() {
        MeasurementDelegate.AdSession session = MeasurementDelegate.AdSession.NO_OP;
        session.impressionOccurred();
        session.finish();
        session.finish(); // idempotent
    }
}
