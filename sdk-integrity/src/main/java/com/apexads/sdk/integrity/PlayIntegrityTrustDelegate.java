package com.apexads.sdk.integrity;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.apexads.sdk.ApexAdsConfig;
import com.apexads.sdk.BuildConfig;
import com.apexads.sdk.core.di.TrustDelegate;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.utils.AdLog;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.StandardIntegrityManager;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

final class PlayIntegrityTrustDelegate implements TrustDelegate {

    private static final String PREFS = "apex_ads_trust_v1";
    private static final long RENEW_BEFORE_SECONDS = 120L;

    private final long cloudProjectNumber;
    private final AtomicBoolean preparing = new AtomicBoolean(false);
    private final AtomicBoolean refreshing = new AtomicBoolean(false);

    private volatile Context context;
    private volatile ApexAdsConfig config;
    private volatile SessionKey sessionKey;
    private volatile StandardIntegrityManager.StandardIntegrityTokenProvider tokenProvider;
    private volatile Lease lease;

    PlayIntegrityTrustDelegate(long cloudProjectNumber) {
        this.cloudProjectNumber = cloudProjectNumber;
    }

    @Override
    public void initialize(@NonNull Context appContext, @NonNull ApexAdsConfig sdkConfig) {
        context = appContext.getApplicationContext();
        config = sdkConfig;
        SdkExecutors.IO.execute(() -> {
            try {
                sessionKey = SessionKey.loadOrCreate();
                restoreLease();
                prepareProvider();
            } catch (Exception e) {
                AdLog.w("Apex Integrity session key failed: %s", e.getMessage());
            }
        });
    }

    @NonNull
    @Override
    public Map<String, String> signedHeaders(@NonNull String method,
                                             @NonNull String url,
                                             @NonNull byte[] body) {
        SessionKey key = sessionKey;
        Lease current = lease;
        long now = System.currentTimeMillis() / 1000L;
        if (key == null || current == null || current.expiresAt <= now) {
            refreshLease();
            return Collections.emptyMap();
        }
        if (current.expiresAt <= now + RENEW_BEFORE_SECONDS) refreshLease();
        try {
            return key.sign(current.token, method, url, body);
        } catch (Exception e) {
            AdLog.w("Apex Integrity request signing failed: %s", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private void prepareProvider() {
        Context ctx = context;
        if (ctx == null || tokenProvider != null || !preparing.compareAndSet(false, true)) return;
        try {
            StandardIntegrityManager manager = IntegrityManagerFactory.createStandard(ctx);
            manager.prepareIntegrityToken(
                            StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                                    .setCloudProjectNumber(cloudProjectNumber)
                                    .build())
                    .addOnSuccessListener(provider -> {
                        tokenProvider = provider;
                        preparing.set(false);
                        refreshLease();
                    })
                    .addOnFailureListener(error -> {
                        preparing.set(false);
                        AdLog.w("Play Integrity warm-up failed: %s", error.getMessage());
                        refreshUnverifiedLease();
                    });
        } catch (RuntimeException e) {
            preparing.set(false);
            AdLog.w("Play Integrity unavailable: %s", e.getMessage());
            refreshUnverifiedLease();
        }
    }

    private void refreshLease() {
        if (sessionKey == null || config == null) return;
        StandardIntegrityManager.StandardIntegrityTokenProvider provider = tokenProvider;
        if (provider == null) {
            prepareProvider();
            return;
        }
        if (!refreshing.compareAndSet(false, true)) return;
        SdkExecutors.IO.execute(() -> {
            try {
                Challenge challenge = requestChallenge();
                String requestHash = requestHash(challenge);
                provider.request(StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                                .setRequestHash(requestHash)
                                .build())
                        .addOnSuccessListener(token -> SdkExecutors.IO.execute(() ->
                                finishLease(challenge, requestHash, token.token())))
                        .addOnFailureListener(error -> {
                            AdLog.w("Play Integrity token failed: %s", error.getMessage());
                            SdkExecutors.IO.execute(() -> finishLease(challenge, requestHash, ""));
                        });
            } catch (Exception e) {
                refreshing.set(false);
                AdLog.w("Apex trust challenge failed: %s", e.getMessage());
            }
        });
    }

    private void refreshUnverifiedLease() {
        if (sessionKey == null || config == null || !refreshing.compareAndSet(false, true)) return;
        SdkExecutors.IO.execute(() -> {
            try {
                Challenge challenge = requestChallenge();
                finishLease(challenge, requestHash(challenge), "");
            } catch (Exception e) {
                refreshing.set(false);
                AdLog.w("Apex unverified lease failed: %s", e.getMessage());
            }
        });
    }

    private void finishLease(Challenge challenge, String requestHash, String integrityToken) {
        try {
            Lease received = requestLease(challenge, requestHash, integrityToken);
            lease = received;
            persistLease(received);
            AdLog.i("Apex trust lease ready [tier=T%d key=%s]", received.tier, sessionKey.keyId());
        } catch (Exception e) {
            AdLog.w("Apex trust lease rejected: %s", e.getMessage());
        } finally {
            refreshing.set(false);
        }
    }

    @NonNull
    private Challenge requestChallenge() throws Exception {
        SessionKey key = sessionKey;
        JSONObject body = new JSONObject();
        body.put("bundle", context.getPackageName());
        body.put("key_id", key.keyId());
        body.put("public_key", key.publicKey());
        body.put("public_key_sha256", key.publicKeyHash());
        body.put("key_security_level", key.securityLevel());
        body.put("hardware_backed", key.hardwareBacked());
        JSONObject response = postJson("/trust/v1/challenge", body);
        return new Challenge(
                response.getString("challenge_id"),
                response.getString("challenge"),
                response.getLong("expires_at"));
    }

    @NonNull
    private Lease requestLease(Challenge challenge,
                               String requestHash,
                               String integrityToken) throws Exception {
        SessionKey key = sessionKey;
        JSONObject body = new JSONObject();
        body.put("challenge_id", challenge.id);
        body.put("request_hash", requestHash);
        body.put("integrity_token", integrityToken);
        body.put("public_key", key.publicKey());
        body.put("public_key_sha256", key.publicKeyHash());
        body.put("key_id", key.keyId());
        body.put("bundle", context.getPackageName());
        body.put("key_security_level", key.securityLevel());
        body.put("hardware_backed", key.hardwareBacked());
        body.put("sdk_version", BuildConfig.SDK_VERSION);
        JSONObject response = postJson("/trust/v1/lease", body);
        return new Lease(
                response.getString("lease"),
                response.getLong("expires_at"),
                response.optInt("trust_tier", 1));
    }

    @NonNull
    private String requestHash(Challenge challenge) {
        SessionKey key = sessionKey;
        return TrustProtocol.leaseRequestHash(
                challenge.id,
                challenge.value,
                context.getPackageName(),
                key.keyId(),
                key.publicKeyHash());
    }

    @NonNull
    private JSONObject postJson(String path, JSONObject body) throws Exception {
        byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
        URL endpoint = new URL(serverOrigin() + path);
        HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("X-ApexAds-Token", config.getAppToken());
            connection.setRequestProperty("X-ApexAds-Version", BuildConfig.SDK_VERSION);
            connection.setConnectTimeout((int) Math.max(10_000, config.getRequestTimeoutMs()));
            connection.setReadTimeout(60_000); // official warm-up guidance has a long tail
            connection.setDoOutput(true);
            connection.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(bytes);
            }
            int status = connection.getResponseCode();
            InputStream input = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            String response = read(input);
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("HTTP " + status + " " + response);
            }
            return new JSONObject(response);
        } finally {
            connection.disconnect();
        }
    }

    @NonNull
    private String serverOrigin() throws Exception {
        URL url = new URL(config.getAdServerUrl());
        int port = url.getPort();
        return url.getProtocol() + "://" + url.getHost()
                + (port >= 0 ? ":" + port : "");
    }

    @NonNull
    private static String read(InputStream input) throws Exception {
        if (input == null) return "";
        try (InputStream source = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = source.read(buffer)) >= 0) out.write(buffer, 0, count);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private void restoreLease() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String keyId = prefs.getString("key_id", "");
        String token = prefs.getString("lease", "");
        long expiresAt = prefs.getLong("expires_at", 0L);
        int tier = prefs.getInt("tier", 1);
        if (sessionKey.keyId().equals(keyId)
                && !token.isEmpty()
                && expiresAt > System.currentTimeMillis() / 1000L + 30L) {
            lease = new Lease(token, expiresAt, tier);
        }
    }

    private void persistLease(Lease value) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("key_id", sessionKey.keyId())
                .putString("lease", value.token)
                .putLong("expires_at", value.expiresAt)
                .putInt("tier", value.tier)
                .apply();
    }

    @Override
    public void close() {
        context = null;
        config = null;
        tokenProvider = null;
        lease = null;
    }

    private static final class Challenge {
        final String id;
        final String value;
        final long expiresAt;

        Challenge(String id, String value, long expiresAt) {
            this.id = id;
            this.value = value;
            this.expiresAt = expiresAt;
        }
    }

    private static final class Lease {
        final String token;
        final long expiresAt;
        final int tier;

        Lease(String token, long expiresAt, int tier) {
            this.token = token;
            this.expiresAt = expiresAt;
            this.tier = tier;
        }
    }
}
