package com.apexads.sdk.core.crashreporter;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.utils.AdLog;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Delivers a {@link CrashEvent} to the Sentry envelope endpoint.
 *
 * Retries up to 3 times with exponential back-off (1s, 2s, 4s).
 * Respects Sentry's 429 rate-limit response — no retry on 429.
 */
final class CrashDelivery {

    private static final int MAX_ATTEMPTS = 3;
    private static final int TIMEOUT_MS = 5_000;
    private static final String CONTENT_TYPE = "application/x-sentry-envelope";

    private final SentryDsn dsn;

    CrashDelivery(@NonNull SentryDsn dsn) {
        this.dsn = dsn;
    }

    void deliver(@NonNull CrashEvent event) {
        String body = event.toEnvelope(dsn.publicKey);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        int delayMs = 1_000;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            int code = postEnvelope(bytes);
            if (code == 200 || code == 201 || code == 202 || code == 204) {
                AdLog.i("CrashReporter: delivered event=%s (attempt %d)", event.eventId, attempt);
                return;
            }
            if (code == 429) {
                AdLog.w("CrashReporter: rate-limited by Sentry, skipping retries");
                return;
            }
            if (attempt < MAX_ATTEMPTS) {
                try { Thread.sleep(delayMs); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); return; }
                delayMs *= 2;
            }
        }
        AdLog.w("CrashReporter: failed to deliver crash event after %d attempts", MAX_ATTEMPTS);
    }

    private int postEnvelope(@NonNull byte[] body) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(dsn.envelopeUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(body.length);
            conn.setRequestProperty("Content-Type", CONTENT_TYPE);
            conn.setRequestProperty("X-Sentry-Auth",
                    "Sentry sentry_version=7, sentry_client=apex-ad-sdk, sentry_key=" + dsn.publicKey);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }
            return conn.getResponseCode();
        } catch (Exception e) {
            AdLog.e(e, "CrashDelivery: network error posting envelope");
            return -1;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
