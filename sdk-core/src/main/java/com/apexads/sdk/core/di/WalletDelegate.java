package com.apexads.sdk.core.di;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.models.AdData;

/**
 * Optional bridge between sdk-core ad renderers and sdk-wallet's Google Wallet integration.
 *
 * Implemented by {@code WalletDelegateImpl} in the sdk-wallet module and registered via
 * {@code WalletAdExtension.install()} in the host application. When not registered,
 * ad renderers function normally without any wallet CTA.
 *
 * sdk-core has zero compile-time dependency on sdk-wallet; the binding is resolved at
 * runtime through {@link ServiceLocator}.
 */
public interface WalletDelegate {

    /** Returns {@code true} if Google Play Services are available on this device. */
    boolean isAvailable(@NonNull Context context);

    /**
     * Attaches a "Save to Google Wallet" bottom panel to a fullscreen interstitial.
     *
     * Called from {@code InterstitialActivity.onCreate} when the ad carries ext.wallet data.
     *
     * @param activity     the interstitial Activity that will receive onActivityResult
     * @param root         the root FrameLayout wrapping the WebView
     * @param walletExtJson raw {@code ext.wallet} JSON string from the bid response
     * @param callback     result callbacks — delivered on the main thread
     */
    void attachToInterstitial(
            @NonNull Activity activity,
            @NonNull ViewGroup root,
            @NonNull String walletExtJson,
            @NonNull WalletEventCallback callback);

    /**
     * Handles an {@code onActivityResult} forwarded from the interstitial Activity.
     *
     * @return {@code true} if the request code matched and the result was consumed
     */
    boolean handleActivityResult(int requestCode, int resultCode);

    /**
     * Attaches a compact "Save to Google Wallet" strip inside an MRECT banner.
     *
     * The strip is added as a bottom-aligned overlay inside {@code container}
     * (the BannerAdView, which is a FrameLayout).
     *
     * @param context   used for view construction and launching WalletResultActivity
     * @param container the BannerAdView
     * @param adData    the loaded AdData (carries walletExtJson)
     * @param callback  result callbacks — delivered on the main thread
     */
    void attachToBanner(
            @NonNull Context context,
            @NonNull ViewGroup container,
            @NonNull AdData adData,
            @NonNull WalletEventCallback callback);

    /** Wallet result callbacks — always delivered on the main thread. */
    interface WalletEventCallback {
        void onPassSaved();
        void onPassCancelled();
        void onPassFailed();
    }
}
