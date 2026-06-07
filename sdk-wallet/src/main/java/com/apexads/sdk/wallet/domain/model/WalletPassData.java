package com.apexads.sdk.wallet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Immutable value object carrying everything needed to save a Google Wallet pass.
 *
 * Parsed from the {@code ext.wallet} block in the OpenRTB bid response.
 * Kept entirely within sdk-wallet — sdk-core has no knowledge of this class.
 */
public final class WalletPassData {

    /** Signed JWT from the advertiser's server — passed directly to Google Wallet. */
    @NonNull public final String passJwt;

    /** Pass type identifier: {@code "coupon"} or {@code "loyalty"}. */
    @NonNull public final String passType;

    /** Advertiser-assigned unique offer ID — used for attribution. */
    @NonNull public final String offerId;

    /** Tracking pixel fired on a successful wallet save. {@code null} if not provided. */
    @Nullable public final String saveTrackingUrl;

    /** Text shown on the native save button inside the ad. */
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

    /**
     * Parses a {@link WalletPassData} from the raw JSON string stored in
     * {@link com.apexads.sdk.core.models.openrtb.BidResponse.BidExt#walletExtJson}.
     *
     * @return a valid instance, or {@code null} if the JSON is absent, malformed,
     *         or missing the required {@code pass_jwt} field.
     */
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
