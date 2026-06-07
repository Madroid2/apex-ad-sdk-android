package com.apexads.sdk.core.crashreporter;

import androidx.annotation.NonNull;

import java.net.URI;

/**
 * Parses a Sentry DSN string into the components needed to POST crash envelopes.
 *
 * DSN format: https://{publicKey}@{host}/{projectId}
 * Envelope endpoint: https://{host}/api/{projectId}/envelope/
 */
final class SentryDsn {

    final String publicKey;
    final String envelopeUrl;

    private SentryDsn(String publicKey, String envelopeUrl) {
        this.publicKey = publicKey;
        this.envelopeUrl = envelopeUrl;
    }

    @NonNull
    static SentryDsn parse(@NonNull String dsn) {
        URI uri = URI.create(dsn.trim());
        String userInfo = uri.getUserInfo();
        if (userInfo == null || userInfo.isEmpty()) {
            throw new IllegalArgumentException("Sentry DSN missing public key in userinfo: " + dsn);
        }
        // userInfo may be "publicKey:secretKey" or just "publicKey"
        String publicKey = userInfo.contains(":") ? userInfo.split(":", 2)[0] : userInfo;

        String path = uri.getPath(); // e.g. "/123456"
        String projectId = path.replaceAll("^/+", "");
        if (projectId.isEmpty()) {
            throw new IllegalArgumentException("Sentry DSN missing project ID: " + dsn);
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        int port = uri.getPort();
        String base = port > 0 ? scheme + "://" + host + ":" + port : scheme + "://" + host;
        String envelopeUrl = base + "/api/" + projectId + "/envelope/";

        return new SentryDsn(publicKey, envelopeUrl);
    }
}
