package com.apexads.sdk.core.network;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;
import com.apexads.sdk.core.utils.AdLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link AdNetworkClient} that orchestrates an ordered list of <b>real</b> demand
 * sources (your own Apex DSP / OpenRTB exchanges) and serves the <b>first genuine
 * fill</b>.
 *
 * <p>On-the-fly selection: every {@link #requestBid} tries each source in priority
 * order. The first source that returns a real, priced, renderable creative wins —
 * so a single request gets multiple real chances to fill, which raises fill rate,
 * impressions and revenue without ever fabricating demand. If a source throws it is
 * skipped and the next is tried; if no source fills, an honest OpenRTB no-fill is
 * returned (never a mock/house placeholder — that decision belongs to the
 * render-layer backfill, not the auction client).
 *
 * <p>Add more demand sources (additional exchange endpoints) to the list to push
 * fill higher. AdMob is <i>not</i> an {@code AdNetworkClient}: it renders its own
 * views via the Google Mobile Ads SDK and never exposes a comparable bid, so it is
 * integrated at the render/adapter layer, not here.
 */
public final class WaterfallAdNetworkClient implements AdNetworkClient {

    private final List<AdNetworkClient> sources;

    public WaterfallAdNetworkClient(@NonNull List<AdNetworkClient> sources) {
        if (sources.isEmpty()) {
            throw new IllegalArgumentException(
                    "WaterfallAdNetworkClient requires at least one demand source");
        }
        this.sources = new ArrayList<>(sources);
    }

    @NonNull
    @Override
    public BidResponse requestBid(@NonNull BidRequest request) throws Exception {
        for (int i = 0; i < sources.size(); i++) {
            try {
                BidResponse response = sources.get(i).requestBid(request);
                if (!isNoFill(response)) {
                    AdLog.d("ApexWaterfall: source[%d] filled — serving real demand", i);
                    return response;
                }
                AdLog.d("ApexWaterfall: source[%d] no-fill — trying next", i);
            } catch (Exception e) {
                AdLog.w("ApexWaterfall: source[%d] error (%s) — trying next", i, e.getMessage());
            }
        }
        AdLog.d("ApexWaterfall: all %d source(s) returned no-fill", sources.size());
        return noFill(request.id);
    }

    @Override
    public void fireTrackingUrl(@NonNull String url) {
        // Tracking fires against the primary (highest-priority) source's transport,
        // matching the previous single-client behaviour.
        sources.get(0).fireTrackingUrl(url);
    }

    /**
     * Returns true when the response carries no priced, renderable creative and the
     * waterfall should advance to the next source.
     */
    public static boolean isNoFill(BidResponse response) {
        if (response == null) return true;
        if (response.seatbid == null || response.seatbid.isEmpty()) return true;

        BidResponse.Bid winner = response.getWinningBid();
        if (winner == null) return true;
        if (winner.adm == null || winner.adm.trim().isEmpty()) return true;
        return winner.price <= 0;
    }

    private static BidResponse noFill(String requestId) {
        BidResponse r = new BidResponse();
        r.id = requestId;
        r.seatbid = Collections.emptyList();
        r.nbr = 2; // OpenRTB no-bid reason — consistent with the SDK's existing convention
        return r;
    }
}
