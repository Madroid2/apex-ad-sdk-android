package com.apexads.sdk.nativeads;

import android.content.Context;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.models.NativeAdPayload;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.tracking.TrackingClient;
import com.apexads.sdk.core.utils.AdUrlHandler;

/**
 * Shared click-through for both native render paths ({@code NativeAdView} and
 * {@code NativeAd.handleClick}). Opens the payload's click destination —
 * web URL, app deep link, or {@code market://} — falling back to the web
 * landing when the deep link cannot resolve.
 *
 * <p>Click accounting: when the destination is a tracker-redirect web URL, the
 * server records the click during the 302. A deep link never touches that
 * redirect, so for {@code OPENED_DEEPLINK} / {@code OPENED_MARKET_WEB} the
 * OpenRTB {@code link.clicktrackers} are fired here instead — exactly one
 * recording per click on every path.
 *
 * <p>Internal SDK API — public only for cross-package access from the render
 * views; not part of the publisher-facing surface.
 */
public final class NativeAdClicks {

    private NativeAdClicks() {}

    public static boolean open(@NonNull Context context,
                               @NonNull NativeAdPayload payload,
                               @NonNull TrackingClient trackingClient,
                               @NonNull String source) {
        int result = AdUrlHandler.openClickThrough(
                context, payload.clickUrl, payload.fallbackUrl, source);
        if (result == AdUrlHandler.OPEN_FAILED) return false;

        if (result == AdUrlHandler.OPENED_DEEPLINK || result == AdUrlHandler.OPENED_MARKET_WEB) {
            for (String url : payload.clickTrackers) {
                SdkExecutors.IO.execute(() -> trackingClient.fireTrackingUrl(url));
            }
        }
        return true;
    }
}
