package com.apexads.sdk.core.network;

import com.apexads.sdk.core.models.openrtb.BidResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Parses an OpenRTB 2.6 bid response JSON string into a {@link BidResponse}.
 *
 * Uses Android's built-in {@code org.json} — no third-party dependency.
 */
final class BidResponseParser {

    private BidResponseParser() {}

    static BidResponse parse(String json) throws JSONException {
        JSONObject root = new JSONObject(json);

        BidResponse resp = new BidResponse();
        resp.id    = root.optString("id", null);
        resp.cur   = root.optString("cur", "USD");
        resp.bidid = root.optString("bidid", null);
        resp.nbr   = root.has("nbr") ? root.getInt("nbr") : null;

        JSONArray seatbids = root.optJSONArray("seatbid");
        if (seatbids != null) {
            resp.seatbid = new ArrayList<>();
            for (int i = 0; i < seatbids.length(); i++) {
                JSONObject sbObj = seatbids.getJSONObject(i);
                BidResponse.SeatBid sb = new BidResponse.SeatBid();
                sb.seat  = sbObj.optString("seat", null);
                sb.group = sbObj.optInt("group", 0);

                JSONArray bidsArr = sbObj.optJSONArray("bid");
                if (bidsArr != null) {
                    sb.bid = new ArrayList<>();
                    for (int j = 0; j < bidsArr.length(); j++) {
                        sb.bid.add(parseBid(bidsArr.getJSONObject(j)));
                    }
                }
                resp.seatbid.add(sb);
            }
        }
        return resp;
    }

    private static BidResponse.Bid parseBid(JSONObject o) throws JSONException {
        BidResponse.Bid bid = new BidResponse.Bid();
        bid.id       = o.optString("id", null);
        bid.impid    = o.optString("impid", null);
        bid.price    = o.optDouble("price", 0.0);
        bid.adid     = o.optString("adid", null);
        bid.nurl     = o.optString("nurl", null);
        bid.burl     = o.optString("burl", null);
        bid.lurl     = o.optString("lurl", null);
        bid.adm      = o.optString("adm", null);
        bid.cid      = o.optString("cid", null);
        bid.crid     = o.optString("crid", null);
        bid.bundle   = o.optString("bundle", null);
        bid.dealid   = o.optString("dealid", null);
        bid.w        = o.has("w")        ? o.getInt("w")        : null;
        bid.h        = o.has("h")        ? o.getInt("h")        : null;
        bid.exp      = o.has("exp")      ? o.getInt("exp")      : null;
        bid.api      = o.has("api")      ? o.getInt("api")      : null;
        bid.protocol = o.has("protocol") ? o.getInt("protocol") : null;

        // Strip empty strings that optString returns for missing keys
        if (bid.adm   != null && bid.adm.isEmpty())   bid.adm   = null;
        if (bid.nurl  != null && bid.nurl.isEmpty())  bid.nurl  = null;
        if (bid.crid  != null && bid.crid.isEmpty())  bid.crid  = null;
        if (bid.id    != null && bid.id.isEmpty())    bid.id    = null;
        if (bid.impid != null && bid.impid.isEmpty()) bid.impid = null;

        JSONObject extObj = o.optJSONObject("ext");
        if (extObj != null) {
            bid.ext = new BidResponse.BidExt();
            JSONObject walletObj = extObj.optJSONObject("wallet");
            if (walletObj != null) {
                bid.ext.walletExtJson = walletObj.toString();
            }
        }

        return bid;
    }
}
