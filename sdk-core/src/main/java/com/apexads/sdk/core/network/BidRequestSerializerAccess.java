package com.apexads.sdk.core.network;

import com.apexads.sdk.core.models.openrtb.BidRequest;

import org.json.JSONException;

public final class BidRequestSerializerAccess {

    private BidRequestSerializerAccess() {}

    public static String serialize(BidRequest req) {
        try {
            return BidRequestSerializer.serialize(req);
        } catch (JSONException e) {
            throw new RuntimeException("BidRequest serialization failed", e);
        }
    }
}
