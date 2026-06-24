package com.apexads.sdk.wallet;

import com.apexads.sdk.core.di.ServiceLocator;
import com.apexads.sdk.core.di.WalletDelegate;

public final class WalletAdExtension {

    private WalletAdExtension() {}

    public static void install() {
        ServiceLocator.register(WalletDelegate.class, new WalletDelegateImpl());
    }
}
