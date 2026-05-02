package com.apexads.sdk.core.models.openrtb;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * OpenRTB 2.6 Bid Response.
 *
 * Plain POJO — JSON parsing handled by BidResponseParser (org.json, no 3p dep).
 */
public class BidResponse {

    public String id;
    public List<SeatBid> seatbid;
    public String bidid;
    public String cur = "USD";
    public String customdata;
    public Integer nbr;
    public Map<String, Object> ext;

    /** Returns the highest-CPM bid across all seat bids, or null if no bids. */
    @Nullable
    public Bid getWinningBid() {
        if (seatbid == null || seatbid.isEmpty()) return null;
        Bid best = null;
        for (SeatBid sb : seatbid) {
            if (sb.bid == null) continue;
            for (Bid b : sb.bid) {
                if (best == null || b.price > best.price) best = b;
            }
        }
        return best;
    }

    // ── SeatBid ───────────────────────────────────────────────────────────────

    public static class SeatBid {
        public List<Bid> bid;
        public String seat;
        public int group = 0;
        public Map<String, Object> ext;
    }

    // ── Bid ───────────────────────────────────────────────────────────────────

    public static class Bid {
        public String id;
        public String impid;
        public double price;
        public String adid;
        public String nurl;
        public String burl;
        public String lurl;
        public String adm;
        public List<String> adomain;
        public String bundle;
        public String cid;
        public String crid;
        public List<String> cat;
        public Integer w;
        public Integer h;
        public Integer exp;
        public String dealid;
        public Integer api;
        public Integer protocol;
        public BidExt ext;
    }

    public static class BidExt {
        public Map<String, Object> prebid;
        /** Raw JSON string of {@code ext.wallet} — parsed by sdk-wallet only. */
        @Nullable public String walletExtJson;
    }
}
