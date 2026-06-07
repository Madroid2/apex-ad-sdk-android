package com.apexads.sdk.core.network;

import com.apexads.sdk.core.models.openrtb.BidRequest;

import org.json.JSONException;

/**
 * Package-bridge that lets modules outside {@code sdk-core} serialise a
 * {@link BidRequest} using the canonical {@link BidRequestSerializer}.
 *
 * <p>{@link BidRequestSerializer} is package-private (final, no external deps).
 * Rather than making it public, we expose this single static method so callers
 * ({@code sdk-inappbidding}, adapters) don't need the serialiser's internals.</p>
 */
public final class BidRequestSerializerAccess {

    private BidRequestSerializerAccess() {}

    /**
     * Serialises {@code req} to an OpenRTB 2.6 JSON string.
     *
     * @throws RuntimeException wrapping {@link JSONException} if serialisation fails.
     */
    public static String serialize(BidRequest req) {
        try {
            return BidRequestSerializer.serialize(req);
        } catch (JSONException e) {
            throw new RuntimeException("BidRequest serialization failed", e);
        }
    }
}
