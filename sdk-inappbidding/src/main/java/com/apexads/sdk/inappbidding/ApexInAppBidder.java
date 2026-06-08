package com.apexads.sdk.inappbidding;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.ApexAds;
import com.apexads.sdk.ApexAdsConfig;
import com.apexads.sdk.BuildConfig;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.request.OpenRTBRequestBuilder;
import com.apexads.sdk.core.utils.AdLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Fetches a real-time bid from the Apex Ad Server's in-app bidding signal endpoint
 * ({@code POST /inapp/v1/signal}) and packages the result as a {@link BidToken}
 * for header-bidding integration with AppLovin MAX or Unity LevelPlay.
 *
 * <h3>Integration flow</h3>
 * <ol>
 *   <li>Implement a mediation adapter for MAX or LevelPlay.</li>
 *   <li>In {@code collectSignal()} / {@code getBidToken()}, call
 *       {@link #fetchBidToken(String, AdFormat, InAppBidListener)}.</li>
 *   <li>In {@code onBidReady(BidToken)}, set the token and CPM on the mediation
 *       SDK's ad unit (e.g. {@code maxAd.setLocalExtraParameter("apex_bid_token", token.token)}).</li>
 *   <li>If {@code onBidFailed}, proceed without Apex — the mediation waterfall
 *       will fall back to the next network.</li>
 * </ol>
 *
 * <h3>Token-path rendering</h3>
 * When Apex wins the waterfall, the mediation SDK calls {@code loadAd()} on the
 * adapter. The adapter should pass {@code BidToken.token} (as {@code user.buyeruid}
 * or a platform-specific extra) in the subsequent {@code POST /inapp/v1/bid} request.
 * The server looks up the pre-cached creative and returns it without re-running the
 * auction — this is the fastest path.
 *
 * <h3>Example — AppLovin MAX adapter</h3>
 * <pre>{@code
 * // In collectSignal():
 * ApexInAppBidder.fetchBidToken("placement-001", AdFormat.BANNER, new InAppBidListener() {
 *     public void onBidReady(BidToken token) {
 *         maxAd.setLocalExtraParameter("apex_bid_token", token.token);
 *         maxAd.setLocalExtraParameter("apex_bid_price", String.valueOf(token.cpmUsd));
 *         callback.onSignalCollected(token.token);
 *     }
 *     public void onBidFailed(AdError error) {
 *         callback.onSignalCollectionFailed(error.getMessage());
 *     }
 * });
 * }</pre>
 */
public final class ApexInAppBidder {

    /** Path of the signal endpoint relative to the ad server base URL. */
    private static final String SIGNAL_PATH = "/inapp/v1/signal";

    private ApexInAppBidder() {}

    /**
     * Fetches a bid token from the Apex Ad Server.
     *
     * <p>The call is made on a background thread; callbacks are delivered on the
     * main thread.</p>
     *
     * @param placementId  Publisher placement ID (forwarded as {@code imp[0].tagid}).
     * @param format       Requested ad format.
     * @param listener     Callback for bid result.
     */
    public static void fetchBidToken(@NonNull String placementId,
                                     @NonNull AdFormat format,
                                     @NonNull InAppBidListener listener) {
        if (!ApexAds.isInitialized()) {
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

    // ── Internal ──────────────────────────────────────────────────────────────

    @Nullable
    private static BidToken doFetch(String placementId, AdFormat format)
            throws IOException, JSONException {

        ApexAdsConfig config = ApexAds.getConfig();

        // Build the signal endpoint URL: replace /openrtb/v1/auction suffix
        // (if present) with /inapp/v1/signal to reach the correct endpoint.
        String base = config.getAdServerUrl();
        int auctionIdx = base.indexOf("/openrtb/v1/auction");
        String signalUrl = (auctionIdx >= 0 ? base.substring(0, auctionIdx) : base) + SIGNAL_PATH;

        // Build OpenRTB bid request (reuses existing builder for full device/consent signals).
        BidRequest req = new OpenRTBRequestBuilder(
                ApexAds.getDeviceInfoProvider(), ApexAds.getConsentManager())
                .adFormat(format)
                .placementId(placementId)
                .build();

        // Serialize using the SDK's own serializer (no 3p dep).
        String body = com.apexads.sdk.core.network.BidRequestSerializerAccess.serialize(req);

        // HTTP POST.
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
            return null; // no fill
        }
        if (status != HttpURLConnection.HTTP_OK) {
            throw new IOException("signal endpoint returned HTTP " + status);
        }

        String responseBody = readAll(conn.getInputStream());
        JSONObject json = new JSONObject(responseBody);

        double priceCpm = json.optDouble("price_cpm", 0.0);
        String token = json.optString("token", null);

        if (token == null || token.isEmpty() || priceCpm <= 0) {
            return null; // malformed response
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
