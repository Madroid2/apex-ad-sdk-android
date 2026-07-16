package com.apexads.sdk.integrity;

import android.util.Base64;

import androidx.annotation.NonNull;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class TrustProtocol {

    static final String ENVELOPE_VERSION = "APEX1";

    private TrustProtocol() {}

    @NonNull
    static String requestTarget(@NonNull String rawUrl) throws Exception {
        URL url = new URL(rawUrl);
        String path = url.getPath();
        if (path == null || path.isEmpty()) path = "/";
        String query = url.getQuery();
        return query == null || query.isEmpty() ? path : path + "?" + query;
    }

    @NonNull
    static String envelope(@NonNull String method,
                           @NonNull String target,
                           long timestamp,
                           @NonNull String nonce,
                           long sequence,
                           @NonNull String bodyHash) {
        return ENVELOPE_VERSION + "\n"
                + method.toUpperCase() + "\n"
                + target + "\n"
                + timestamp + "\n"
                + nonce + "\n"
                + sequence + "\n"
                + bodyHash;
    }

    @NonNull
    static String leaseRequestHash(@NonNull String challengeId,
                                   @NonNull String challenge,
                                   @NonNull String bundle,
                                   @NonNull String keyId,
                                   @NonNull String publicKeyHash) {
        String binding = "APEX-TRUST-V1\n" + challengeId + "\n" + challenge + "\n"
                + bundle + "\n" + keyId + "\n" + publicKeyHash;
        return sha256(binding.getBytes(StandardCharsets.UTF_8));
    }

    @NonNull
    static String sha256(@NonNull byte[] value) {
        try {
            return base64Url(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @NonNull
    static String base64Url(@NonNull byte[] value) {
        return Base64.encodeToString(value,
                Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }
}
