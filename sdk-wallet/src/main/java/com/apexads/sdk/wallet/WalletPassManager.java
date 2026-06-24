package com.apexads.sdk.wallet;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.utils.AdLog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.pay.Pay;
import com.google.android.gms.pay.PayClient;

public final class WalletPassManager {

    public static final int REQUEST_CODE_SAVE_PASS = 0xBA11;

    private WalletPassManager() {}

    public static boolean isAvailable(@NonNull Context context) {
        try {
            int status = GoogleApiAvailability.getInstance()
                    .isGooglePlayServicesAvailable(context);
            return status == ConnectionResult.SUCCESS;
        } catch (Exception e) {
            AdLog.w(e, "WalletPassManager: Play Services availability check failed");
            return false;
        }
    }

    public static void savePass(@NonNull Activity activity, @NonNull String passJwt) {
        try {
            PayClient payClient = Pay.getClient(activity);
            payClient.savePassesJwt(passJwt, activity, REQUEST_CODE_SAVE_PASS);
            AdLog.d("WalletPassManager: savePassesJwt dispatched");
        } catch (Exception e) {

            throw new RuntimeException("WalletPassManager: savePass failed — " + e.getMessage(), e);
        }
    }
}
