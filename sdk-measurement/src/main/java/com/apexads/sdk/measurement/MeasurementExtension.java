package com.apexads.sdk.measurement;

import com.apexads.sdk.core.di.MeasurementDelegate;
import com.apexads.sdk.core.di.ServiceLocator;

/**
 * Activates Open Measurement (OMID) viewability across all display formats.
 * Mirrors {@code WalletAdExtension}: one call after {@code ApexAds.init()}.
 *
 * <pre>{@code
 * ApexAds.init(this, config);
 * MeasurementExtension.install();
 * }</pre>
 *
 * <p>OMID bid-request signaling ({@code api=7}, {@code source.ext.omidpn/omidpv})
 * activates only when the bundled OM SDK is certified and ready — see
 * {@link OmidMeasurementDelegate}.</p>
 */
public final class MeasurementExtension {

    private MeasurementExtension() {}

    public static void install() {
        ServiceLocator.register(MeasurementDelegate.class, new OmidMeasurementDelegate());
    }
}
