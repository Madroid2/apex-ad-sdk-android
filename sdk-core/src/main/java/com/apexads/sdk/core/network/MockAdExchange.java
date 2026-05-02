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

        boolean walletSupported = imp.ext != null
                && Boolean.TRUE.equals(imp.ext.get("wallet_supported"));
        boolean isMrect = imp.banner != null
                && imp.banner.h != null && imp.banner.h >= 250;

        if (imp.video != null) {
            bid = buildVastBid(imp.id, imp.bidfloor);
        } else if (imp.nativeObject != null) {
            bid = buildNativeBid(imp.id, imp.bidfloor);
        } else if (imp.instl == 1) {
            bid = buildInterstitialBid(imp.id, imp.bidfloor, walletSupported);
        } else {
            bid = buildBannerBid(imp.id, imp.bidfloor, walletSupported && isMrect);
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

    private BidResponse.Bid buildBannerBid(String impId, double floor, boolean withWallet) {
        BidResponse.Bid bid = newBid(impId, Math.max(floor + 0.05, 1.50));
        bid.crid = "banner-001";
        bid.adm = withWallet ? MRECT_WALLET_HTML : BANNER_HTML;
        bid.nurl = "https://track.apexads.mock/win?type=banner";
        bid.w = withWallet ? 300 : 320;
        bid.h = withWallet ? 250 : 50;
        if (withWallet) bid.ext = buildWalletExt();
        return bid;
    }

    private BidResponse.Bid buildInterstitialBid(String impId, double floor, boolean withWallet) {
        BidResponse.Bid bid = newBid(impId, Math.max(floor + 0.10, 4.50));
        bid.crid = "interstitial-001";
        bid.adm = withWallet ? WALLET_HTML : INTERSTITIAL_HTML;
        bid.nurl = "https://track.apexads.mock/win?type=interstitial";
        bid.w = 320;
        bid.h = 480;
        if (withWallet) bid.ext = buildWalletExt();
        return bid;
    }

    private BidResponse.BidExt buildWalletExt() {
        BidResponse.BidExt ext = new BidResponse.BidExt();
        ext.walletExtJson =
            "{\"pass_jwt\":\"REPLACE_WITH_SIGNED_JWT\"," +
            "\"pass_type\":\"coupon\"," +
            "\"offer_id\":\"APEX-DEMO-20OFF-2025\"," +
            "\"save_tracking_url\":\"https://track.apexads.mock/wallet_save?oid=APEX-DEMO-20OFF-2025\"," +
            "\"cta_text\":\"Save Coupon to Google Wallet\"}";
        return ext;
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

    // 10-second Big Buck Bunny clip — 720p H.264, ~1 MB, stable CDN purpose-built for media testing.
    // Previous URL (storage.googleapis.com/gvabox) now returns 403.
    private static final String VAST_XML =
        "<VAST version=\"4.0\">" +
        "<Ad id=\"mock-preroll-001\"><InLine>" +
        "<AdSystem version=\"1.0\">ApexAd Mock DSP</AdSystem>" +
        "<AdTitle>ApexAd Video Demo</AdTitle>" +
        "<Impression id=\"imp-1\"><![CDATA[https://track.apexads.mock/impression?t=video]]></Impression>" +
        "<Creatives><Creative id=\"1\" sequence=\"1\"><Linear>" +
        "<Duration>00:00:10</Duration>" +
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
        "<MediaFile delivery=\"progressive\" type=\"video/mp4\" width=\"1280\" height=\"720\" bitrate=\"1024\">" +
        "<![CDATA[https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4]]>" +
        "</MediaFile>" +
        "</MediaFiles>" +
        "</Linear></Creative></Creatives>" +
        "</InLine></Ad></VAST>";

    private static final String WALLET_HTML =
        "<!DOCTYPE html><html><head>" +
        "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
        "<style>" +
        "*{margin:0;padding:0;box-sizing:border-box;}" +
        "body{width:100vw;height:100vh;" +
        "background:linear-gradient(160deg,#1565c0 0%,#0d47a1 60%,#01579b 100%);" +
        "display:flex;flex-direction:column;align-items:center;justify-content:center;" +
        "font-family:Arial,sans-serif;padding:24px 24px 120px;}" +
        ".store{color:rgba(255,255,255,0.75);font-size:13px;letter-spacing:1px;" +
        "text-transform:uppercase;margin-bottom:12px;}" +
        ".pct{color:#fff;font-size:72px;font-weight:900;line-height:1;}" +
        ".off{color:#ffca28;font-size:18px;font-weight:700;letter-spacing:2px;margin-bottom:20px;}" +
        ".divider{width:48px;height:2px;background:rgba(255,255,255,0.3);margin-bottom:20px;}" +
        ".title{color:#fff;font-size:20px;font-weight:700;text-align:center;margin-bottom:10px;}" +
        ".desc{color:rgba(255,255,255,0.8);font-size:13px;text-align:center;" +
        "line-height:1.6;margin-bottom:20px;}" +
        ".terms{color:rgba(255,255,255,0.45);font-size:10px;text-align:center;}" +
        ".badge{position:absolute;top:14px;right:14px;" +
        "background:rgba(255,255,255,0.12);color:rgba(255,255,255,0.6);" +
        "font-size:9px;padding:3px 8px;border-radius:10px;letter-spacing:0.5px;}" +
        "</style></head>" +
        "<body>" +
        "<span class=\"badge\">ADVERTISEMENT</span>" +
        "<div class=\"store\">ApexAd Demo Store</div>" +
        "<div class=\"pct\">20%</div>" +
        "<div class=\"off\">OFF YOUR NEXT ORDER</div>" +
        "<div class=\"divider\"></div>" +
        "<div class=\"title\">Exclusive Member Coupon</div>" +
        "<div class=\"desc\">Save this coupon to your Google Wallet and show it at checkout — in-store or online.</div>" +
        "<div class=\"terms\">Min. spend $50 · Valid until 31 Dec 2025 · Single use per customer</div>" +
        "</body></html>";

    private static final String MRECT_WALLET_HTML =
        "<!DOCTYPE html><html><head>" +
        "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
        "<style>" +
        "*{margin:0;padding:0;box-sizing:border-box;}" +
        "body{width:300px;height:250px;" +
        "background:linear-gradient(160deg,#1565c0 0%,#0d47a1 60%,#01579b 100%);" +
        "display:flex;flex-direction:column;align-items:center;justify-content:center;" +
        "font-family:Arial,sans-serif;padding:12px 16px 56px;}" +
        ".store{color:rgba(255,255,255,0.75);font-size:10px;letter-spacing:1px;" +
        "text-transform:uppercase;margin-bottom:8px;}" +
        ".pct{color:#fff;font-size:52px;font-weight:900;line-height:1;}" +
        ".off{color:#ffca28;font-size:13px;font-weight:700;letter-spacing:2px;margin-bottom:10px;}" +
        ".title{color:#fff;font-size:14px;font-weight:700;text-align:center;margin-bottom:6px;}" +
        ".desc{color:rgba(255,255,255,0.8);font-size:10px;text-align:center;line-height:1.5;}" +
        ".badge{position:absolute;top:8px;right:8px;" +
        "background:rgba(255,255,255,0.12);color:rgba(255,255,255,0.6);" +
        "font-size:8px;padding:2px 6px;border-radius:8px;}" +
        "</style></head>" +
        "<body>" +
        "<span class=\"badge\">AD</span>" +
        "<div class=\"store\">ApexAd Demo Store</div>" +
        "<div class=\"pct\">20%</div>" +
        "<div class=\"off\">OFF YOUR NEXT ORDER</div>" +
        "<div class=\"title\">Exclusive Member Coupon</div>" +
        "<div class=\"desc\">Show at checkout · Valid until 31 Dec 2025</div>" +
        "</body></html>";

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
