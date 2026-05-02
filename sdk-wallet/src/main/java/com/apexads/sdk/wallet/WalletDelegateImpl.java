package com.apexads.sdk.wallet;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.di.WalletDelegate;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.utils.AdLog;

import java.lang.ref.WeakReference;

/**
 * Concrete implementation of {@link WalletDelegate}.
 *
 * Handles all Google Wallet interactions on behalf of sdk-interstitial and sdk-banner.
 * Neither of those modules has any compile-time dependency on sdk-wallet or
 * play-services-wallet — everything is resolved at runtime via ServiceLocator.
 *
 * Registered by {@link WalletAdExtension#install()}.
 */
final class WalletDelegateImpl implements WalletDelegate {

    /**
     * Holds state for the currently active interstitial wallet save flow.
     * Cleared by {@link #handleActivityResult} and by the session's own cleanup.
     */
    private static volatile WalletSession activeInterstitialSession;

    // ── WalletDelegate ────────────────────────────────────────────────────────

    @Override
    public boolean isAvailable(@NonNull Context context) {
        return WalletPassManager.isAvailable(context);
    }

    @Override
    public void attachToInterstitial(
            @NonNull Activity activity,
            @NonNull ViewGroup root,
            @NonNull String walletExtJson,
            @NonNull WalletEventCallback callback) {
        WalletPassData passData = WalletPassData.fromJson(walletExtJson);
        if (passData == null) {
            AdLog.d("WalletDelegateImpl: ext.wallet present but unparseable — skip interstitial CTA");
            return;
        }

        // Bottom overlay panel
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(0xF2000000);
        panel.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 16));
        panel.setGravity(Gravity.CENTER_HORIZONTAL);

        // Status text — revealed after wallet result
        TextView tvStatus = new TextView(activity);
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setTextSize(13f);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setVisibility(View.GONE);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(0, 0, 0, dp(activity, 8));
        panel.addView(tvStatus, statusParams);

        // Save button
        TextView tvBtn = new TextView(activity);
        tvBtn.setText(passData.ctaText);
        tvBtn.setTextColor(Color.WHITE);
        tvBtn.setTextSize(15f);
        tvBtn.setTypeface(tvBtn.getTypeface(), Typeface.BOLD);
        tvBtn.setGravity(Gravity.CENTER);
        tvBtn.setBackgroundColor(0xFF1565C0);
        tvBtn.setPadding(dp(activity, 20), dp(activity, 14), dp(activity, 20), dp(activity, 14));

        if (!WalletPassManager.isAvailable(activity)) {
            tvBtn.setEnabled(false);
            tvBtn.setAlpha(0.4f);
            tvBtn.setText("Google Wallet not available");
        }

        // Session created before onClick so the closure can reference it safely
        WalletSession session = new WalletSession(passData, tvBtn, tvStatus, callback);
        activeInterstitialSession = session;

        tvBtn.setOnClickListener(v -> {
            if (session.saveAttempted) return;
            session.saveAttempted = true;
            tvBtn.setEnabled(false);
            try {
                WalletPassManager.savePass(activity, passData.passJwt);
            } catch (RuntimeException e) {
                AdLog.e(e, "WalletDelegateImpl: savePass threw in interstitial");
                session.saveAttempted = false;
                tvBtn.setEnabled(true);
                showStatus(tvStatus, "Could not open Google Wallet", 0xFF7F0000);
                SdkExecutors.MAIN.post(callback::onPassFailed);
            }
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        panel.addView(tvBtn, btnParams);

        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        panelParams.gravity = Gravity.BOTTOM;
        root.addView(panel, panelParams);

        AdLog.d("WalletDelegateImpl: wallet panel attached to interstitial offer=%s", passData.offerId);
    }

    @Override
    public boolean handleActivityResult(int requestCode, int resultCode) {
        if (requestCode != WalletPassManager.REQUEST_CODE_SAVE_PASS) return false;
        WalletSession session = activeInterstitialSession;
        activeInterstitialSession = null;
        if (session == null) return true;

        TextView tvBtn = session.tvBtnRef.get();
        TextView tvStatus = session.tvStatusRef.get();

        if (resultCode == Activity.RESULT_OK) {
            AdLog.i("WalletDelegateImpl: pass saved offer=%s", session.passData.offerId);
            fireTracking(session.passData.saveTrackingUrl);
            if (tvStatus != null) showStatus(tvStatus, "Saved to Google Wallet ✓", 0xFF1B5E20);
            if (tvBtn != null) tvBtn.setEnabled(false);
            SdkExecutors.MAIN.post(session.callback::onPassSaved);
        } else if (resultCode == Activity.RESULT_CANCELED) {
            AdLog.d("WalletDelegateImpl: user cancelled interstitial wallet save");
            if (tvStatus != null) showStatus(tvStatus, "", Color.TRANSPARENT);
            if (tvBtn != null) {
                tvBtn.setEnabled(true);
                session.saveAttempted = false;
            }
            SdkExecutors.MAIN.post(session.callback::onPassCancelled);
        } else {
            AdLog.w("WalletDelegateImpl: interstitial wallet save failed resultCode=%d", resultCode);
            if (tvStatus != null) showStatus(tvStatus, "Could not save — try again", 0xFF7F0000);
            if (tvBtn != null) {
                tvBtn.setEnabled(true);
                session.saveAttempted = false;
            }
            SdkExecutors.MAIN.post(session.callback::onPassFailed);
        }
        return true;
    }

    @Override
    public void attachToBanner(
            @NonNull Context context,
            @NonNull ViewGroup container,
            @NonNull AdData adData,
            @NonNull WalletEventCallback callback) {
        if (adData.walletExtJson == null) return;
        WalletPassData passData = WalletPassData.fromJson(adData.walletExtJson);
        if (passData == null) {
            AdLog.d("WalletDelegateImpl: banner ext.wallet present but unparseable — skip CTA");
            return;
        }

        // Compact CTA strip — bottom-aligned overlay inside the BannerAdView
        TextView tvBtn = new TextView(context);
        tvBtn.setText(passData.ctaText);
        tvBtn.setTextColor(Color.WHITE);
        tvBtn.setTextSize(13f);
        tvBtn.setTypeface(tvBtn.getTypeface(), Typeface.BOLD);
        tvBtn.setGravity(Gravity.CENTER);
        tvBtn.setBackgroundColor(0xFF1565C0);
        tvBtn.setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10));

        if (!WalletPassManager.isAvailable(context)) {
            tvBtn.setEnabled(false);
            tvBtn.setAlpha(0.4f);
            tvBtn.setText("Google Wallet not available");
        }

        tvBtn.setOnClickListener(v -> {
            tvBtn.setEnabled(false);
            WalletResultActivity.launch(context, passData.passJwt,
                    passData.saveTrackingUrl, callback);
        });

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM;
        container.addView(tvBtn, params);

        AdLog.d("WalletDelegateImpl: wallet CTA strip attached to MRECT offer=%s", passData.offerId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void showStatus(@NonNull TextView tv, @NonNull String msg, int bgColor) {
        if (msg.isEmpty()) {
            tv.setVisibility(View.GONE);
            return;
        }
        tv.setText(msg);
        tv.setBackgroundColor(bgColor);
        tv.setPadding(8, 6, 8, 6);
        tv.setVisibility(View.VISIBLE);
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

    private static int dp(@NonNull Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    // ── Session ───────────────────────────────────────────────────────────────

    /**
     * Carries interstitial wallet state across the Activity → Google Wallet → Activity
     * round-trip. WeakReferences on the Views prevent leaks if the Activity is destroyed
     * before {@link #handleActivityResult} fires.
     */
    private static final class WalletSession {
        final WalletPassData passData;
        final WeakReference<TextView> tvBtnRef;
        final WeakReference<TextView> tvStatusRef;
        final WalletEventCallback callback;
        volatile boolean saveAttempted = false;

        WalletSession(WalletPassData p, TextView b, TextView s, WalletEventCallback c) {
            passData = p;
            tvBtnRef = new WeakReference<>(b);
            tvStatusRef = new WeakReference<>(s);
            callback = c;
        }
    }
}
