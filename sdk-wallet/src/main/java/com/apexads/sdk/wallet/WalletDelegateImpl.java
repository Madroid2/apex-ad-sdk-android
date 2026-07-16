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
import com.apexads.sdk.internal.ApexSdkRuntime;
import com.apexads.sdk.wallet.presentation.view.WalletResultActivity;

import java.lang.ref.WeakReference;

final class WalletDelegateImpl implements WalletDelegate {

    private static volatile WalletSession activeInterstitialSession;

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

        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(0xF2000000);
        panel.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 16));
        panel.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView tvStatus = new TextView(activity);
        tvStatus.setTextColor(Color.WHITE);
        tvStatus.setTextSize(13f);
        tvStatus.setGravity(Gravity.CENTER);
        tvStatus.setVisibility(View.GONE);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.setMargins(0, 0, 0, dp(activity, 8));
        panel.addView(tvStatus, statusParams);

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

        WalletSession session = new WalletSession(passData, tvBtn, tvStatus, callback);
        activeInterstitialSession = session;

        tvBtn.setOnClickListener(v -> {
            if (session.saveAttempted) return;
            session.saveAttempted = true;
            tvBtn.setEnabled(false);

            if (ApexSdkRuntime.isInitialized() && ApexSdkRuntime.getConfig().isTestMode()) {
                AdLog.d("WalletDelegateImpl: test mode — simulating wallet save for offer=%s",
                        passData.offerId);
                showStatus(tvStatus, "✓ Saved to Google Wallet (test mode)", 0xFF1B5E20);
                fireTracking(passData.saveTrackingUrl);
                SdkExecutors.MAIN.post(callback::onPassSaved);
                return;
            }

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

            if (ApexSdkRuntime.isInitialized() && ApexSdkRuntime.getConfig().isTestMode()) {
                AdLog.d("WalletDelegateImpl: test mode — simulating banner wallet save offer=%s",
                        passData.offerId);
                tvBtn.setText("✓ Saved (test mode)");
                fireTracking(passData.saveTrackingUrl);
                SdkExecutors.MAIN.post(callback::onPassSaved);
                return;
            }
            WalletResultActivity.launch(context, passData.passJwt,
                    passData.saveTrackingUrl, callback);
        });

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM;
        container.addView(tvBtn, params);

        AdLog.d("WalletDelegateImpl: wallet CTA strip attached to MRECT offer=%s", passData.offerId);
    }

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
                ApexSdkRuntime.getTrackingClient().fireTrackingUrl(url);
            } catch (Exception ignored) {}
        });
    }

    private static int dp(@NonNull Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

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
