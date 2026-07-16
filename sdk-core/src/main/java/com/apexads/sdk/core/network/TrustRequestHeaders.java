package com.apexads.sdk.core.network;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.apexads.sdk.core.di.ServiceLocator;
import com.apexads.sdk.core.di.TrustDelegate;
import com.apexads.sdk.core.utils.AdLog;

/** Applies optional APEX1 trust-envelope headers without coupling core to Play Integrity. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class TrustRequestHeaders {

    private TrustRequestHeaders() {}

    public static void apply(@NonNull HttpURLConnection connection,
                             @NonNull String method,
                             @NonNull String url,
                             @NonNull byte[] body) {
        for (Map.Entry<String, String> header : forRequest(method, url, body).entrySet()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
    }

    @NonNull
    static Map<String, String> forRequest(@NonNull String method,
                                          @NonNull String url,
                                          @NonNull byte[] body) {
        TrustDelegate delegate = ServiceLocator.getOptional(TrustDelegate.class);
        if (delegate == null) return Collections.emptyMap();
        try {
            return delegate.signedHeaders(method, url, body);
        } catch (RuntimeException e) {
            // Trust warm-up must never crash the host app. The server will treat
            // this request as T1 and restrict it to house/no-fill inventory.
            AdLog.w("Apex trust envelope unavailable: %s", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
