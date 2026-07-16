package com.apexads.sdk.integrity;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.di.ServiceLocator;
import com.apexads.sdk.core.di.TrustDelegate;
import com.apexads.sdk.internal.ApexSdkRuntime;

/** Installs optional Play Integrity-backed Apex trust sessions. */
public final class ApexIntegrityExtension {

    private ApexIntegrityExtension() {}

    /**
     * Installs the module before or after {@code ApexAds.init()}.
     *
     * @param cloudProjectNumber numeric Google Cloud project number linked in
     *                           Play Console or Play SDK Console
     */
    public static void install(long cloudProjectNumber) {
        if (cloudProjectNumber <= 0) {
            throw new IllegalArgumentException("cloudProjectNumber must be positive");
        }
        PlayIntegrityTrustDelegate delegate = new PlayIntegrityTrustDelegate(cloudProjectNumber);
        ServiceLocator.register(TrustDelegate.class, delegate);
        if (ApexSdkRuntime.isInitialized()) {
            delegate.initialize(ApexSdkRuntime.getContext(), ApexSdkRuntime.getConfig());
        }
    }

    /** Returns whether a trust implementation is registered with the SDK. */
    public static boolean isInstalled() {
        return ServiceLocator.isRegistered(TrustDelegate.class);
    }
}
