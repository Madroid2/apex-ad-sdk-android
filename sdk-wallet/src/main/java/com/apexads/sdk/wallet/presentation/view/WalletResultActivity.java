package com.apexads.sdk.wallet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.di.WalletDelegate;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.utils.AdLog;

/**
 * Transparent, short-lived Activity that drives the Google Wallet save flow for
 * MRECT banner ads.
 *
 * <p>Banner ads live inside a publisher-owned Activity, so the SDK cannot intercept
 * {@code onActivityResult} there. This transparent Activity acts as a proxy:
 * <ol>
 *   <li>Launched by {@link WalletDelegateImpl#attachToBanner} when the user taps the CTA.</li>
 *   <li>Immediately calls {@link WalletPassManager#savePass}, which starts the Google Wallet
 *       intent for result.</li>
 *   <li>Receives the wallet result in {@link #onActivityResult}, fires the appropriate
 *       callback, then finishes — leaving the publisher's UI untouched.</li>
 * </ol>
 *
 * <p>Memory safety:
 * <ul>
 *   <li>Static slots are cleared in {@link #onDestroy}.</li>
 *   <li>No Activity reference is stored statically.</li>
 * </ul>
 */
public final class WalletResultActivity extends Activity {

    // ── Static handoff slots — cleared in onDestroy ───────────────────────────
    private static volatile String pendingPassJwt;
    private static volatile String pendingTrackingUrl;
    private static volatile WalletDelegate.WalletEventCallback pendingCallback;

    // ── Instance state ────────────────────────────────────────────────────────
    private boolean delivered = false;

    // ── Static launch method ──────────────────────────────────────────────────

    static void launch(
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

    // ── Lifecycle ─────────────────────────────────────────────────────────────

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
        // Guard against the Activity being destroyed without onActivityResult firing.
        deliver(RESULT_CANCELED);
        pendingPassJwt = null;
        pendingTrackingUrl = null;
        pendingCallback = null;
        super.onDestroy();
    }

    // ── Internal ──────────────────────────────────────────────────────────────

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
            } catch (Exception ignored) {
                // Tracking pixel failure must never surface to the publisher.
            }
        });
    }
}
