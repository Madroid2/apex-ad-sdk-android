package com.apexads.sdk.core.network;

import static com.google.common.truth.Truth.assertThat;

import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

public class MockAdExchangeTest {

    private final MockAdExchange exchange = new MockAdExchange();

    @Test
    public void requestBid_withoutImpression_returnsOpenRtbNoFill() throws Exception {
        BidRequest request = new BidRequest();
        request.id = "req-no-imp";

        BidResponse response = exchange.requestBid(request);

        assertThat(response.id).isEqualTo("req-no-imp");
        assertThat(response.nbr).isEqualTo(2);
        assertThat(response.getWinningBid()).isNull();
    }

    @Test
    public void requestBid_banner_returnsPricedHtmlBidAboveFloor() throws Exception {
        BidResponse.Bid bid = exchange.requestBid(requestWith(bannerImp("imp", 2.0, 320, 50)))
                .getWinningBid();

        assertThat(bid).isNotNull();
        assertThat(bid.impid).isEqualTo("imp");
        assertThat(bid.price).isAtLeast(2.05);
        assertThat(bid.crid).isEqualTo("banner-001");
        assertThat(bid.adm).contains("ApexAd SDK");
        assertThat(bid.w).isEqualTo(320);
        assertThat(bid.h).isEqualTo(50);
        assertThat(bid.ext).isNull();
    }

    @Test
    public void requestBid_mrectWithWalletSupport_returnsWalletExtension() throws Exception {
        BidRequest.Impression imp = bannerImp("imp", 1.0, 300, 250);
        imp.ext = new HashMap<>();
        imp.ext.put("wallet_supported", true);

        BidResponse.Bid bid = exchange.requestBid(requestWith(imp)).getWinningBid();

        assertThat(bid).isNotNull();
        assertThat(bid.w).isEqualTo(300);
        assertThat(bid.h).isEqualTo(250);
        assertThat(bid.ext).isNotNull();
        assertThat(bid.ext.walletExtJson).contains("pass_jwt");
    }

    @Test
    public void requestBid_interstitialWithWalletSupport_returnsFullscreenWalletBid() throws Exception {
        BidRequest.Impression imp = bannerImp("imp", 4.6, 320, 480);
        imp.instl = 1;
        imp.ext = new HashMap<>();
        imp.ext.put("wallet_supported", true);

        BidResponse.Bid bid = exchange.requestBid(requestWith(imp)).getWinningBid();

        assertThat(bid).isNotNull();
        assertThat(bid.price).isWithin(0.0001).of(4.7);
        assertThat(bid.crid).isEqualTo("interstitial-001");
        assertThat(bid.ext.walletExtJson).contains("Save Coupon to Google Wallet");
    }

    @Test
    public void requestBid_video_returnsVastBid() throws Exception {
        BidRequest.Impression imp = new BidRequest.Impression();
        imp.id = "video-imp";
        imp.bidfloor = 7.9;
        imp.video = new BidRequest.Video();

        BidResponse.Bid bid = exchange.requestBid(requestWith(imp)).getWinningBid();

        assertThat(bid).isNotNull();
        assertThat(bid.price).isAtLeast(8.4);
        assertThat(bid.protocol).isEqualTo(7);
        assertThat(bid.adm).contains("<VAST");
    }

    @Test
    public void requestBid_native_returnsNativeMarkup() throws Exception {
        BidRequest.Impression imp = new BidRequest.Impression();
        imp.id = "native-imp";
        imp.bidfloor = 2.9;
        imp.nativeObject = new BidRequest.NativeObject();

        BidResponse.Bid bid = exchange.requestBid(requestWith(imp)).getWinningBid();

        assertThat(bid).isNotNull();
        assertThat(bid.price).isAtLeast(3.1);
        assertThat(bid.crid).isEqualTo("native-001");
        assertThat(bid.adm).contains("\"native\"");
    }

    private static BidRequest requestWith(BidRequest.Impression imp) {
        BidRequest request = new BidRequest();
        request.id = "req";
        request.imp = Collections.singletonList(imp);
        return request;
    }

    private static BidRequest.Impression bannerImp(String id, double floor, int width, int height) {
        BidRequest.Banner banner = new BidRequest.Banner();
        banner.w = width;
        banner.h = height;

        BidRequest.Impression imp = new BidRequest.Impression();
        imp.id = id;
        imp.bidfloor = floor;
        imp.banner = banner;
        return imp;
    }
}
