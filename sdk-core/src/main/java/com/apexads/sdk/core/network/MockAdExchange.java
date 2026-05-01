package com.apexads.sdk.core.network;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;

import java.util.Collections;
import java.util.UUID;

/**
 * In-process mock ad exchange for development and CI testing.
 *
 * Returns realistic OpenRTB 2.6 responses with inline HTML creatives,
 * a VAST 4.0 video tag (public Google sample MP4), and IAB Native 1.2 JSON.
 * No live server, API key, or network beyond the video file itself.
 *
 * Wire up in DemoApplication:
 * <pre>{@code
 * ServiceLocator.register(AdNetworkClient.class, new MockAdExchange());
 * }</pre>
 */
public class MockAdExchange implements AdNetworkClient {

    private static final long SIMULATED_LATENCY_MS = 120L;

    @NonNull
    @Override
    public BidResponse requestBid(@NonNull BidRequest request) throws Exception {
        Thread.sleep(SIMULATED_LATENCY_MS);

        if (request.imp == null || request.imp.isEmpty()) {
            return noFill(request.id);
        }

        BidRequest.Impression imp = request.imp.get(0);
        BidResponse.Bid bid;

        if (imp.video != null) {
            bid = buildVastBid(imp.id, imp.bidfloor);
        } else if (imp.nativeObject != null) {
            bid = buildNativeBid(imp.id, imp.bidfloor);
        } else if (imp.instl == 1) {
            bid = buildInterstitialBid(imp.id, imp.bidfloor);
        } else {
            bid = buildBannerBid(imp.id, imp.bidfloor);
        }

        BidResponse.SeatBid seatBid = new BidResponse.SeatBid();
        seatBid.bid = Collections.singletonList(bid);
        seatBid.seat = "mock-dsp";

        BidResponse response = new BidResponse();
        response.id = request.id;
        response.seatbid = Collections.singletonList(seatBid);
        response.cur = "USD";
        return response;
    }

    @Override
    public void fireTrackingUrl(@NonNull String url) {
        // No-op in mock — tracking URLs are logged but not fired.
    }

    private BidResponse noFill(String requestId) {
        BidResponse r = new BidResponse();
        r.id = requestId;
        r.seatbid = Collections.emptyList();
        r.nbr = 2;
        return r;
    }

    private BidResponse.Bid buildBannerBid(String impId, double floor) {
        BidResponse.Bid bid = newBid(impId, Math.max(floor + 0.05, 1.50));
        bid.crid = "banner-001";
        bid.adm = BANNER_HTML;
        bid.nurl = "https://track.apexads.mock/win?type=banner";
        bid.w = 320;
        bid.h = 50;
        return bid;
    }

    private BidResponse.Bid buildInterstitialBid(String impId, double floor) {
        BidResponse.Bid bid = newBid(impId, Math.max(floor + 0.10, 4.50));
        bid.crid = "interstitial-001";
        bid.adm = INTERSTITIAL_HTML;
        bid.nurl = "https://track.apexads.mock/win?type=interstitial";
        bid.w = 320;
        bid.h = 480;
        return bid;
    }

    private BidResponse.Bid buildVastBid(String impId, double floor) {
        BidResponse.Bid bid = newBid(impId, Math.max(floor + 0.50, 8.00));
        bid.crid = "video-001";
        bid.adm = VAST_XML;
        bid.nurl = "https://track.apexads.mock/win?type=video";
        bid.protocol = 7; // VAST 4.0
        bid.w = 640;
        bid.h = 360;
        return bid;
    }

    private BidResponse.Bid buildNativeBid(String impId, double floor) {
        BidResponse.Bid bid = newBid(impId, Math.max(floor + 0.20, 3.00));
        bid.crid = "native-001";
        bid.adm = NATIVE_JSON;
        bid.nurl = "https://track.apexads.mock/win?type=native";
        return bid;
    }

    private BidResponse.Bid newBid(String impId, double price) {
        BidResponse.Bid bid = new BidResponse.Bid();
        bid.id = UUID.randomUUID().toString();
        bid.impid = impId;
        bid.price = price;
        return bid;
    }

    // ── Creative assets ───────────────────────────────────────────────────────

    private static final String BANNER_HTML =
        "<!DOCTYPE html><html><head>" +
        "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
        "<style>*{margin:0;padding:0;box-sizing:border-box;}" +
        "body{width:320px;height:50px;background:linear-gradient(135deg,#667eea,#764ba2);" +
        "display:flex;align-items:center;justify-content:center;" +
        "font-family:Arial,sans-serif;cursor:pointer;}" +
        ".ad{color:#fff;font-size:14px;font-weight:bold;letter-spacing:0.5px;}" +
        ".badge{font-size:9px;opacity:0.7;margin-left:6px;border:1px solid rgba(255,255,255,0.5);" +
        "padding:1px 4px;border-radius:3px;}" +
        "</style></head>" +
        "<body onclick=\"window.open('https://github.com','_blank')\">" +
        "<div class=\"ad\">🚀 ApexAd SDK — Banner Demo " +
        "<span class=\"badge\">AD</span></div></body></html>";

    private static final String INTERSTITIAL_HTML =
        "<!DOCTYPE html><html><head>" +
        "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
        "<style>*{margin:0;padding:0;box-sizing:border-box;}" +
        "body{width:100vw;height:100vh;background:linear-gradient(135deg,#f093fb,#f5576c);" +
        "display:flex;flex-direction:column;align-items:center;justify-content:center;" +
        "font-family:Arial,sans-serif;}" +
        "h1{color:#fff;font-size:32px;font-weight:900;margin-bottom:12px;}" +
        "p{color:rgba(255,255,255,0.9);font-size:16px;margin-bottom:32px;" +
        "text-align:center;max-width:280px;}" +
        ".cta{background:#fff;color:#f5576c;padding:14px 36px;border-radius:50px;" +
        "font-size:16px;font-weight:bold;border:none;cursor:pointer;}" +
        ".badge{position:absolute;top:16px;right:16px;" +
        "font-size:10px;color:rgba(255,255,255,0.6);}" +
        "</style></head><body>" +
        "<span class=\"badge\">Advertisement</span>" +
        "<h1>ApexAd SDK</h1>" +
        "<p>Full-stack Android ad SDK: OpenRTB · MRAID 3.0 · VAST 4.0</p>" +
        "<button class=\"cta\" onclick=\"ApexMRAID.close()\">Get Started →</button>" +
        "</body></html>";

    private static final String VAST_XML =
        "<VAST version=\"4.0\">" +
        "<Ad id=\"mock-preroll-001\"><InLine>" +
        "<AdSystem version=\"1.0\">ApexAd Mock DSP</AdSystem>" +
        "<AdTitle>ApexAd Video Demo</AdTitle>" +
        "<Impression id=\"imp-1\"><![CDATA[https://track.apexads.mock/impression?t=video]]></Impression>" +
        "<Creatives><Creative id=\"1\" sequence=\"1\"><Linear>" +
        "<Duration>00:00:15</Duration>" +
        "<TrackingEvents>" +
        "<Tracking event=\"start\"><![CDATA[https://track.apexads.mock/video?event=start]]></Tracking>" +
        "<Tracking event=\"firstQuartile\"><![CDATA[https://track.apexads.mock/video?event=q1]]></Tracking>" +
        "<Tracking event=\"midpoint\"><![CDATA[https://track.apexads.mock/video?event=mid]]></Tracking>" +
        "<Tracking event=\"thirdQuartile\"><![CDATA[https://track.apexads.mock/video?event=q3]]></Tracking>" +
        "<Tracking event=\"complete\"><![CDATA[https://track.apexads.mock/video?event=complete]]></Tracking>" +
        "</TrackingEvents>" +
        "<VideoClicks>" +
        "<ClickThrough><![CDATA[https://github.com]]></ClickThrough>" +
        "<ClickTracking id=\"ct1\"><![CDATA[https://track.apexads.mock/click?type=video]]></ClickTracking>" +
        "</VideoClicks>" +
        "<MediaFiles>" +
        "<MediaFile delivery=\"progressive\" type=\"video/mp4\" width=\"640\" height=\"360\" bitrate=\"500\">" +
        "<![CDATA[https://storage.googleapis.com/gvabox/media/samples/stock.mp4]]>" +
        "</MediaFile>" +
        "</MediaFiles>" +
        "</Linear></Creative></Creatives>" +
        "</InLine></Ad></VAST>";

    private static final String NATIVE_JSON =
        "{\"native\":{\"ver\":\"1.2\"," +
        "\"link\":{\"url\":\"https://github.com\"," +
        "\"clicktrackers\":[\"https://track.apexads.mock/click?type=native\"]}," +
        "\"imptrackers\":[\"https://track.apexads.mock/impression?t=native\"]," +
        "\"assets\":[" +
        "{\"id\":1,\"required\":1,\"title\":{\"text\":\"ApexAd SDK: Programmatic Advertising\"}}," +
        "{\"id\":2,\"required\":1,\"img\":{\"type\":3,\"url\":\"https://picsum.photos/seed/apexad/1200/627\",\"w\":1200,\"h\":627}}," +
        "{\"id\":3,\"img\":{\"type\":1,\"url\":\"https://picsum.photos/seed/apexlogo/80/80\",\"w\":80,\"h\":80}}," +
        "{\"id\":4,\"required\":1,\"data\":{\"type\":2,\"value\":\"Full-stack Android ad SDK with OpenRTB, MRAID 3.0, VAST 4.0 support.\"}}," +
        "{\"id\":5,\"data\":{\"type\":1,\"value\":\"ApexAd\"}}," +
        "{\"id\":6,\"data\":{\"type\":12,\"value\":\"Learn More\"}}]}}";
}
