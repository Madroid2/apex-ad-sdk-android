package com.apexads.sdk.core.network;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class WaterfallAdNetworkClientTest {

    @Test
    public void constructor_emptySources_throws() {
        assertThrows(IllegalArgumentException.class, () -> new WaterfallAdNetworkClient(Collections.emptyList()));
    }

    @Test
    public void requestBid_returnsFirstRenderablePricedCreative() throws Exception {
        FakeClient noFill = new FakeClient(noFill("req"));
        FakeClient fill = new FakeClient(fill("req", "creative", 1.0));
        FakeClient neverCalled = new FakeClient(fill("req", "late", 2.0));

        BidResponse response = new WaterfallAdNetworkClient(Arrays.asList(noFill, fill, neverCalled))
                .requestBid(request("req"));

        assertThat(response.getWinningBid().adm).isEqualTo("creative");
        assertThat(noFill.calls.get()).isEqualTo(1);
        assertThat(fill.calls.get()).isEqualTo(1);
        assertThat(neverCalled.calls.get()).isEqualTo(0);
    }

    @Test
    public void requestBid_skipsThrowingSourceAndContinues() throws Exception {
        FakeClient throwing = new FakeClient(new RuntimeException("boom"));
        FakeClient fill = new FakeClient(fill("req", "creative", 1.0));

        BidResponse response = new WaterfallAdNetworkClient(Arrays.asList(throwing, fill))
                .requestBid(request("req"));

        assertThat(response.getWinningBid().adm).isEqualTo("creative");
    }

    @Test
    public void requestBid_allSourcesNoFill_returnsOpenRtbNoFill() throws Exception {
        BidResponse response = new WaterfallAdNetworkClient(Arrays.asList(
                new FakeClient(noFill("req")),
                new FakeClient(fill("req", "   ", 2.0)),
                new FakeClient(fill("req", "creative", 0.0))))
                .requestBid(request("req"));

        assertThat(response.id).isEqualTo("req");
        assertThat(response.nbr).isEqualTo(2);
        assertThat(response.getWinningBid()).isNull();
    }

    @Test
    public void isNoFill_handlesNullEmptyMissingMarkupAndZeroPrice() {
        assertThat(WaterfallAdNetworkClient.isNoFill(null)).isTrue();
        assertThat(WaterfallAdNetworkClient.isNoFill(noFill("req"))).isTrue();
        assertThat(WaterfallAdNetworkClient.isNoFill(fill("req", "", 1.0))).isTrue();
        assertThat(WaterfallAdNetworkClient.isNoFill(fill("req", "markup", 0.0))).isTrue();
        assertThat(WaterfallAdNetworkClient.isNoFill(fill("req", "markup", 0.1))).isFalse();
    }

    @Test
    public void fireTrackingUrl_delegatesOnlyToPrimary() {
        FakeClient primary = new FakeClient(noFill("req"));
        FakeClient secondary = new FakeClient(noFill("req"));

        new WaterfallAdNetworkClient(Arrays.asList(primary, secondary)).fireTrackingUrl("https://track");

        assertThat(primary.trackingUrl).isEqualTo("https://track");
        assertThat(secondary.trackingUrl).isNull();
    }

    private static BidRequest request(String id) {
        BidRequest request = new BidRequest();
        request.id = id;
        return request;
    }

    private static BidResponse noFill(String id) {
        BidResponse response = new BidResponse();
        response.id = id;
        response.seatbid = Collections.emptyList();
        return response;
    }

    private static BidResponse fill(String id, String adm, double price) {
        BidResponse.Bid bid = new BidResponse.Bid();
        bid.id = "bid";
        bid.impid = "imp";
        bid.adm = adm;
        bid.price = price;
        BidResponse.SeatBid seatBid = new BidResponse.SeatBid();
        seatBid.bid = Collections.singletonList(bid);
        BidResponse response = new BidResponse();
        response.id = id;
        response.seatbid = Collections.singletonList(seatBid);
        return response;
    }

    private static final class FakeClient implements AdNetworkClient {
        final AtomicInteger calls = new AtomicInteger();
        final BidResponse response;
        final Exception error;
        String trackingUrl;

        FakeClient(BidResponse response) {
            this.response = response;
            this.error = null;
        }

        FakeClient(Exception error) {
            this.response = null;
            this.error = error;
        }

        @Override
        public BidResponse requestBid(BidRequest request) throws Exception {
            calls.incrementAndGet();
            if (error != null) throw error;
            return response;
        }

        @Override
        public void fireTrackingUrl(String url) {
            trackingUrl = url;
        }
    }
}
