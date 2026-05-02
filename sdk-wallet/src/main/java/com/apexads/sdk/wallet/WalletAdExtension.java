package com.apexads.sdk.wallet;

import com.apexads.sdk.core.di.ServiceLocator;
import com.apexads.sdk.core.di.WalletDelegate;

/**
 * Registers the wallet feature into the SDK's service locator.
 *
 * Call {@link #install()} once in your {@code Application.onCreate}, after
 * {@code ApexAds.init()}, to enable the "Save to Google Wallet" CTA inside
 * Interstitial ads and MRECT Banner ads:
 *
 * <pre>{@code
 * ApexAds.init(this, config);
 * WalletAdExtension.install();
 * }</pre>
 *
 * If you do NOT call {@code install()}, ads load and display normally — the
 * wallet CTA is simply absent. There is no crash or fallback error.
 */
public final class WalletAdExtension {

    private WalletAdExtension() {}

    /** Registers {@link WalletDelegateImpl} so wallet CTAs are activated at runtime. */
    public static void install() {
        ServiceLocator.register(WalletDelegate.class, new WalletDelegateImpl());
    }
}
