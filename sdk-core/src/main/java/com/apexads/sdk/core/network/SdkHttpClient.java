package com.apexads.sdk.core.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Minimal HTTP client using {@link java.net.HttpURLConnection}.
 *
 * Zero third-party dependencies. Both methods are blocking — always call
 * from {@link SdkExecutors#IO}.
 */
final class SdkHttpClient {

    private static final String JSON_TYPE      = "application/json; charset=utf-8";
    private static final int    CONNECT_TIMEOUT = 10_000;
    private static final int    READ_TIMEOUT    = 10_000;

    private SdkHttpClient() {}

    /**
     * HTTP POST with a JSON body. Returns the response body string.
     *
     * @throws IOException on network error or non-2xx HTTP status.
     */
    static String post(String urlStr,
                       String jsonBody,
                       String appToken,
                       String sdkVersion,
                       int timeoutMs) throws IOException {
        HttpURLConnection conn = open(urlStr, timeoutMs);
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", JSON_TYPE);
            conn.setRequestProperty("Accept", JSON_TYPE);
            conn.setRequestProperty("X-ApexAds-Token", appToken);
            conn.setRequestProperty("X-ApexAds-Version", sdkVersion);
            conn.setDoOutput(true);

            byte[] body = jsonBody.getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(body.length);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code + " from ad server");
            }
            return readStream(conn.getInputStream());
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Fire-and-forget HTTP GET. Silently ignores errors — tracking pixels must
     * not surface exceptions to the publisher.
     */
    static void fireGet(String urlStr) {
        try {
            HttpURLConnection conn = open(urlStr, READ_TIMEOUT);
            try {
                conn.setRequestMethod("GET");
                conn.getResponseCode(); // ensure request is sent
            } finally {
                conn.disconnect();
            }
        } catch (IOException ignored) {
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static HttpURLConnection open(String urlStr, int timeoutMs) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(Math.max(CONNECT_TIMEOUT, timeoutMs));
        conn.setReadTimeout(Math.max(READ_TIMEOUT, timeoutMs));
        return conn;
    }

    private static String readStream(InputStream is) throws IOException {
        try {
            byte[] buf  = new byte[4096];
            StringBuilder sb = new StringBuilder();
            int n;
            while ((n = is.read(buf)) != -1) {
                sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
            }
            return sb.toString();
        } finally {
            is.close();
        }
    }
}
