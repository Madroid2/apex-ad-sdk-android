package com.apexads.sdk.core.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Result-reporting HTTP transport for tracking events and quality reports.
 * Unlike the fire-and-forget pixel path, callers here need to know whether
 * delivery succeeded so failed events can be retried from the offline queue.
 */
public final class TrackingTransport {

    private static final int TIMEOUT_MS = 10_000;

    private TrackingTransport() {}

    /** GETs a tracking URL. Returns true on any 2xx/3xx response. */
    public static boolean sendGet(@NonNull String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            try {
                conn.setRequestMethod("GET");
                int code = conn.getResponseCode();
                return code >= 200 && code < 400;
            } finally {
                conn.disconnect();
            }
        } catch (IOException e) {
            return false;
        }
    }

    /** POSTs a JSON body. Returns true on any 2xx response. */
    public static boolean sendJson(@NonNull String urlStr, @NonNull String json) {
        return sendJson(urlStr, json, null);
    }

    /**
     * POSTs a JSON body with the publisher credential used by auxiliary SDK
     * reports. Tracking callback URLs remain independently HMAC-authenticated
     * and continue to use the two-argument overload.
     */
    public static boolean sendJson(@NonNull String urlStr,
                                   @NonNull String json,
                                   @Nullable String appToken) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            try {
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                if (appToken != null && !appToken.trim().isEmpty()) {
                    conn.setRequestProperty("X-ApexAds-Token", appToken);
                }
                conn.setDoOutput(true);
                byte[] body = json.getBytes(StandardCharsets.UTF_8);
                TrustRequestHeaders.apply(conn, "POST", urlStr, body);
                conn.setFixedLengthStreamingMode(body.length);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body);
                }
                int code = conn.getResponseCode();
                return code >= 200 && code < 300;
            } finally {
                conn.disconnect();
            }
        } catch (IOException e) {
            return false;
        }
    }
}
