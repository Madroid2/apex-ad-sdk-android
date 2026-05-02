package com.apexads.sdk.wallet;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.apexads.sdk.ApexAds;
import com.apexads.sdk.core.utils.AdLog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.wallet.PayClient;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;

/**
 * Thin wrapper around the Google Wallet {@link PayClient}.
 *
 * Stateless — no Activity references are stored, preventing memory leaks.
 * The wallet save result is delivered to the calling Activity via
 * {@link Activity#onActivityResult(int, int, android.content.Intent)}.
 */
final class WalletPassManager {

    /** Request code used with {@link Activity#startActivityForResult}. */
    static final int REQUEST_CODE_SAVE_PASS = 0xBA11;

    private WalletPassManager() {}

    /**
     * Returns {@code true} if Google Play Services are available and the
     * {@link PayClient} can be created on this device.
     */
    static boolean isAvailable(@NonNull Context context) {
        try {
            int status = GoogleApiAvailability.getInstance()
                    .isGooglePlayServicesAvailable(context);
            return status == ConnectionResult.SUCCESS;
        } catch (Exception e) {
            AdLog.w(e, "WalletPassManager: Play Services availability check failed");
            return false;
        }
    }

    /**
     * Initiates the Google Wallet save flow for the given signed JWT.
     *
     * The result is delivered asynchronously via
     * {@code Activity.onActivityResult(REQUEST_CODE_SAVE_PASS, resultCode, data)}.
     * Call {@link Activity#RESULT_OK} check in the calling Activity to
     * determine success.
     *
     * @param activity   the foreground Activity that will receive the result
     * @param passJwt    signed Google Wallet pass JWT from the advertiser server
     */
    static void savePass(@NonNull Activity activity, @NonNull String passJwt) {
        try {
            int environment = ApexAds.getConfig().isTestMode()
                    ? WalletConstants.ENVIRONMENT_TEST
                    : WalletConstants.ENVIRONMENT_PRODUCTION;
            Wallet.WalletOptions options = new Wallet.WalletOptions.Builder()
                    .setEnvironment(environment)
                    .build();
            PayClient payClient = Wallet.getPayClient(activity, options);
            payClient.savePassesJwt(passJwt, activity, REQUEST_CODE_SAVE_PASS);
            AdLog.d("WalletPassManager: savePassesJwt dispatched (env=%s)",
                    environment == WalletConstants.ENVIRONMENT_TEST ? "TEST" : "PRODUCTION");
        } catch (Exception e) {
            // savePassesJwt throws if the intent cannot be started.
            // The caller will not receive onActivityResult in this case, so we
            // Re-throw as RuntimeException so callers (WalletDelegateImpl, WalletResultActivity) can handle it.
            throw new RuntimeException("WalletPassManager: savePass failed — " + e.getMessage(), e);
        }
    }
}
