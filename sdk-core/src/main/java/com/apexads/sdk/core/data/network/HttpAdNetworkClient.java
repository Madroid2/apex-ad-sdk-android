package com.apexads.sdk.core.network;

import androidx.annotation.NonNull;

import com.apexads.sdk.ApexAdsConfig;
import com.apexads.sdk.BuildConfig;
import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;
import com.apexads.sdk.core.utils.AdLog;

/**
 * {@link AdNetworkClient} backed by {@link java.net.HttpURLConnection}.
 *
 * Zero third-party dependencies: org.json (Android built-in) for
 * serialization, HttpURLConnection for transport. Both methods are blocking —
 * callers must invoke from {@link SdkExecutors#IO}.
 */
public final class HttpAdNetworkClient implements AdNetworkClient {

    private final ApexAdsConfig config;

    public HttpAdNetworkClient(@NonNull ApexAdsConfig config) {
        this.config = config;
    }

    @NonNull
    @Override
    public BidResponse requestBid(@NonNull BidRequest request) throws Exception {
        String json = BidRequestSerializer.serialize(request);

        if (config.isDebugLogging()) {
            AdLog.d("ApexNet → POST %s body=%d bytes", config.getAdServerUrl(), json.length());
        }

        String responseJson = SdkHttpClient.post(
                config.getAdServerUrl(),
                json,
                config.getAppToken(),
                BuildConfig.SDK_VERSION,
                (int) config.getRequestTimeoutMs());

        if (config.isDebugLogging()) {
            AdLog.d("ApexNet ← %d chars", responseJson.length());
        }

        BidResponse response = BidResponseParser.parse(responseJson);
        if (response == null) {
            throw new AdRequestException("Failed to parse BidResponse JSON");
        }
        return response;
    }

    @Override
    public void fireTrackingUrl(@NonNull String url) {
        // Non-throwing by contract — tracking failure must not surface to publisher.
        SdkHttpClient.fireGet(url);
        AdLog.d("Tracking pixel fired: %s", url);
    }

    public static final class AdRequestException extends Exception {
        public AdRequestException(String message) { super(message); }
    }
}
