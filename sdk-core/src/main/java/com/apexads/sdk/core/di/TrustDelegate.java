package com.apexads.sdk.core.di;

import android.content.Context;

import androidx.annotation.NonNull;

import com.apexads.sdk.ApexAdsConfig;

import java.util.Map;

/**
 * Optional device-trust feature contract.
 *
 * <p>The implementation lives in {@code sdk-integrity} so the zero-dependency
 * core does not pull Google Play Integrity into every publisher app. Once a
 * short-lived trust lease is available, the delegate signs auction and event
 * requests with its Android Keystore session key.</p>
 */
public interface TrustDelegate extends SdkFeature {

    void initialize(@NonNull Context appContext, @NonNull ApexAdsConfig config);

    /** Returns HTTP headers for an APEX1 envelope, or an empty map while unverified. */
    @NonNull
    Map<String, String> signedHeaders(@NonNull String method,
                                      @NonNull String url,
                                      @NonNull byte[] body);

    default void close() {}
}
