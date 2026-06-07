package com.apexads.sdk.core.models;

import static com.google.common.truth.Truth.assertThat;

import com.apexads.sdk.core.models.openrtb.BidResponse;

import org.junit.Test;

import java.util.Collections;

public class AdDataTest {

    @Test
    public void fromBid_banner_mapsBidFieldsAndTtl() {
        BidResponse.Bid bid = new BidResponse.Bid();
        bid.id = "bid-1";
        bid.impid = "imp-1";
        bid.adm = "<html/>";
        bid.nurl = "https://win";
        bid.crid = "creative-1";
        bid.price = 2.45;
        bid.w = 320;
        bid.h = 50;

        long before = System.currentTimeMillis();
        AdData data = AdData.fromBid("request-1", bid, AdFormat.BANNER, "EUR", 60);

        assertThat(data.requestId).isEqualTo("request-1");
        assertThat(data.impressionId).isEqualTo("imp-1");
        assertThat(data.bidId).isEqualTo("bid-1");
        assertThat(data.adMarkup).isEqualTo("<html/>");
        assertThat(data.winNoticeUrl).isEqualTo("https://win");
        assertThat(data.creativeId).isEqualTo("creative-1");
        assertThat(data.adFormat).isEqualTo(AdFormat.BANNER);
        assertThat(data.width).isEqualTo(320);
        assertThat(data.height).isEqualTo(50);
        assertThat(data.cpm).isEqualTo(2.45);
        assertThat(data.currency).isEqualTo("EUR");
        assertThat(data.expiresAt).isAtLeast(before + 60_000L);
        assertThat(data.vastXml).isNull();
    }

    @Test
    public void fromBid_video_setsVastXmlAndDefaultsMissingFields() {
        BidResponse.Bid bid = new BidResponse.Bid();
        bid.id = "bid-1";
        bid.impid = "imp-1";
        bid.price = 1.0;
        bid.adm = "<VAST/>";

        AdData data = AdData.fromBid("request-1", bid, AdFormat.REWARDED_VIDEO, "USD", 1);

        assertThat(data.width).isEqualTo(0);
        assertThat(data.height).isEqualTo(0);
        assertThat(data.vastXml).isEqualTo("<VAST/>");
        assertThat(data.adMarkup).isEqualTo("<VAST/>");
    }

    @Test
    public void fromBid_walletExt_preservedAsRawJson() {
        BidResponse.Bid bid = new BidResponse.Bid();
        bid.id = "bid-1";
        bid.impid = "imp-1";
        bid.adm = "<html/>";
        bid.ext = new BidResponse.BidExt();
        bid.ext.walletExtJson = "{\"pass_jwt\":\"jwt\"}";

        AdData data = AdData.fromBid("request-1", bid, AdFormat.INTERSTITIAL, "USD", 1);

        assertThat(data.walletExtJson).isEqualTo("{\"pass_jwt\":\"jwt\"}");
    }

    @Test
    public void withNativePayload_copiesOriginalFieldsAndAddsPayload() {
        AdData original = new AdData.Builder()
                .requestId("req")
                .impressionId("imp")
                .bidId("bid")
                .adMarkup("{}")
                .winNoticeUrl("https://win")
                .creativeId("cr")
                .adFormat(AdFormat.NATIVE)
                .width(1)
                .height(2)
                .cpm(3.0)
                .currency("USD")
                .expiresAt(123L)
                .walletExtJson("{wallet}")
                .build();
        NativeAdPayload payload = new NativeAdPayload(
                "Title", "Body", null, "https://image", "Install", "Brand", "https://click",
                Collections.singletonList("https://imp"));

        AdData next = original.withNativePayload(payload);

        assertThat(next).isNotSameInstanceAs(original);
        assertThat(next.requestId).isEqualTo("req");
        assertThat(next.nativePayload).isEqualTo(payload);
        assertThat(next.walletExtJson).isEqualTo("{wallet}");
    }

    @Test
    public void isExpired_checksCurrentTime() {
        AdData expired = new AdData.Builder().expiresAt(System.currentTimeMillis() - 1L).build();
        AdData fresh = new AdData.Builder().expiresAt(System.currentTimeMillis() + 60_000L).build();

        assertThat(expired.isExpired()).isTrue();
        assertThat(fresh.isExpired()).isFalse();
    }
}
