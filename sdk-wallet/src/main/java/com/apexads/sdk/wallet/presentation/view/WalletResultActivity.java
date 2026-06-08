package com.apexads.sdk.wallet.presentation.view;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.di.WalletDelegate;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.utils.AdLog;
import com.apexads.sdk.wallet.WalletPassManager;

public final class WalletResultActivity extends Activity {

    private static volatile String pendingPassJwt;
    private static volatile String pendingTrackingUrl;
    private static volatile WalletDelegate.WalletEventCallback pendingCallback;

    private boolean delivered = false;

    public static void launch(
            @NonNull Context context,
            @NonNull String passJwt,
            @Nullable String trackingUrl,
            @NonNull WalletDelegate.WalletEventCallback callback) {
        pendingPassJwt = passJwt;
        pendingTrackingUrl = trackingUrl;
        pendingCallback = callback;
        Intent intent = new Intent(context, WalletResultActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String passJwt = pendingPassJwt;
        if (passJwt == null) {
            AdLog.w("WalletResultActivity: launched with null passJwt — finishing");
            finish();
            return;
        }
        try {
            WalletPassManager.savePass(this, passJwt);
        } catch (RuntimeException e) {
            AdLog.e(e, "WalletResultActivity: savePass threw");
            deliver(RESULT_FIRST_USER);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == WalletPassManager.REQUEST_CODE_SAVE_PASS) {
            deliver(resultCode);
        }
        finish();
    }

    @Override
    protected void onDestroy() {

        deliver(RESULT_CANCELED);
        pendingPassJwt = null;
        pendingTrackingUrl = null;
        pendingCallback = null;
        super.onDestroy();
    }

    private void deliver(int resultCode) {
        if (delivered) return;
        delivered = true;
        WalletDelegate.WalletEventCallback cb = pendingCallback;
        String trackingUrl = pendingTrackingUrl;
        if (cb == null) return;
        if (resultCode == RESULT_OK) {
            AdLog.i("WalletResultActivity: pass saved successfully");
            fireTracking(trackingUrl);
            SdkExecutors.MAIN.post(cb::onPassSaved);
        } else if (resultCode == RESULT_CANCELED) {
            AdLog.d("WalletResultActivity: user cancelled wallet save");
            SdkExecutors.MAIN.post(cb::onPassCancelled);
        } else {
            AdLog.w("WalletResultActivity: wallet save failed resultCode=%d", resultCode);
            SdkExecutors.MAIN.post(cb::onPassFailed);
        }
    }

    private static void fireTracking(@Nullable String url) {
        if (url == null) return;
        SdkExecutors.IO.execute(() -> {
            try {
                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);
                conn.setRequestMethod("GET");
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception ignored) {}
        });
    }
}
