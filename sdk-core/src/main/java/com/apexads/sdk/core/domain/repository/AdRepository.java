package com.apexads.sdk.core.domain.repository;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;

/**
 * Data-layer contract for fetching ad creatives from the exchange.
 *
 * <p>Separating the network layer behind this interface lets the ViewModel stay
 * agnostic of HTTP / OpenRTB serialisation details, and makes unit-testing trivial:
 * inject a stub {@code AdRepository} that returns canned {@link AdData} without
 * a real network.
 *
 * <p>Implementations must dispatch network work onto a background thread and post
 * results to the <strong>main thread</strong> before invoking the callbacks.
 */
public interface AdRepository {

    /** Callback for a successful auction. */
    interface OnSuccess {
        void onSuccess(@NonNull AdData adData);
    }

    /** Callback for a failed auction. */
    interface OnFailure {
        void onFailure(@NonNull AdError error);
    }

    /**
     * Runs an OpenRTB auction for the given parameters.
     *
     * @param format      Ad format (BANNER, INTERSTITIAL, REWARDED_VIDEO, NATIVE).
     * @param size        Requested creative dimensions.
     * @param placementId Publisher placement identifier sent in the bid request.
     * @param bidFloor    Minimum acceptable CPM; 0.0 means no floor.
     * @param onSuccess   Called on the main thread when a winning bid is received.
     * @param onFailure   Called on the main thread when no fill or an error occurs.
     */
    void loadAd(
            @NonNull AdFormat format,
            @NonNull AdSize size,
            @NonNull String placementId,
            double bidFloor,
            @NonNull OnSuccess onSuccess,
            @NonNull OnFailure onFailure
    );
}
