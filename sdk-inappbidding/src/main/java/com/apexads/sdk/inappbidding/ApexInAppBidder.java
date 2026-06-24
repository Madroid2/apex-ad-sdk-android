package com.apexads.sdk.inappbidding;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.ApexAdsConfig;
import com.apexads.sdk.BuildConfig;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.request.OpenRTBRequestBuilder;
import com.apexads.sdk.core.utils.AdLog;
import com.apexads.sdk.internal.ApexSdkRuntime;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class ApexInAppBidder {

    private static final String SIGNAL_PATH = "/inapp/v1/signal";

    private ApexInAppBidder() {}

    public static void fetchBidToken(@NonNull String placementId,
                                     @NonNull AdFormat format,
                                     @NonNull InAppBidListener listener) {
        if (!ApexSdkRuntime.isInitialized()) {
            SdkExecutors.MAIN.post(() ->
                    listener.onBidFailed(new AdError.Network("ApexAds SDK not initialized", null)));
            return;
        }

        SdkExecutors.IO.execute(() -> {
            try {
                BidToken token = doFetch(placementId, format);
                if (token == null) {
                    SdkExecutors.MAIN.post(() -> listener.onBidFailed(new AdError.NoFill()));
                    return;
                }
                AdLog.i("ApexInAppBidder: bid ready cpm=%.3f placement=%s token=%s",
                        token.cpmUsd, placementId, token.token);
                SdkExecutors.MAIN.post(() -> listener.onBidReady(token));
            } catch (Exception e) {
                AdLog.e(e, "ApexInAppBidder: signal fetch failed");
                SdkExecutors.MAIN.post(() -> listener.onBidFailed(new AdError.Network(e.getMessage(), e)));
            }
        });
    }

    @Nullable
    private static BidToken doFetch(String placementId, AdFormat format)
            throws IOException, JSONException {

        ApexAdsConfig config = ApexSdkRuntime.getConfig();

        String base = config.getAdServerUrl();
        int auctionIdx = base.indexOf("/openrtb/v1/auction");
        String signalUrl = (auctionIdx >= 0 ? base.substring(0, auctionIdx) : base) + SIGNAL_PATH;

        BidRequest req = new OpenRTBRequestBuilder(
                ApexSdkRuntime.getDeviceInfoProvider(), ApexSdkRuntime.getConsentManager())
                .adFormat(format)
                .placementId(placementId)
                .build();

        String body = com.apexads.sdk.core.network.BidRequestSerializerAccess.serialize(req);

        HttpURLConnection conn = (HttpURLConnection) new URL(signalUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-ApexAds-Token", config.getAppToken());
        conn.setRequestProperty("x-openrtb-version", "2.6");
        conn.setConnectTimeout((int) config.getRequestTimeoutMs());
        conn.setReadTimeout((int) config.getRequestTimeoutMs());
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status == HttpURLConnection.HTTP_NO_CONTENT || status == 204) {
            return null;
        }
        if (status != HttpURLConnection.HTTP_OK) {
            throw new IOException("signal endpoint returned HTTP " + status);
        }

        String responseBody = readAll(conn.getInputStream());
        JSONObject json = new JSONObject(responseBody);

        double priceCpm = json.optDouble("price_cpm", 0.0);
        String token = json.optString("token", null);

        if (token == null || token.isEmpty() || priceCpm <= 0) {
            return null;
        }

        return new BidToken(placementId, priceCpm, token);
    }

    private static String readAll(InputStream is) throws IOException {
        byte[] buf = new byte[4096];
        StringBuilder sb = new StringBuilder();
        int n;
        while ((n = is.read(buf)) != -1) {
            sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }
}
