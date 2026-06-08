package com.apexads.sdk.wallet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public final class WalletPassData {

    @NonNull public final String passJwt;

    @NonNull public final String passType;

    @NonNull public final String offerId;

    @Nullable public final String saveTrackingUrl;

    @NonNull public final String ctaText;

    private WalletPassData(
            @NonNull String passJwt,
            @NonNull String passType,
            @NonNull String offerId,
            @Nullable String saveTrackingUrl,
            @NonNull String ctaText) {
        this.passJwt = passJwt;
        this.passType = passType;
        this.offerId = offerId;
        this.saveTrackingUrl = saveTrackingUrl;
        this.ctaText = ctaText;
    }

    @Nullable
    public static WalletPassData fromJson(@Nullable String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            JSONObject o = new JSONObject(json);
            String jwt = o.optString("pass_jwt", null);
            if (jwt == null || jwt.isEmpty()) return null;
            String trackingUrl = o.optString("save_tracking_url", null);
            if (trackingUrl != null && trackingUrl.isEmpty()) trackingUrl = null;
            return new WalletPassData(
                    jwt,
                    o.optString("pass_type", "coupon"),
                    o.optString("offer_id", ""),
                    trackingUrl,
                    o.optString("cta_text", "Save to Google Wallet"));
        } catch (JSONException e) {
            return null;
        }
    }

    @Override
    @NonNull
    public String toString() {
        return "WalletPassData{offerId='" + offerId + "', passType='" + passType + "'}";
    }
}
