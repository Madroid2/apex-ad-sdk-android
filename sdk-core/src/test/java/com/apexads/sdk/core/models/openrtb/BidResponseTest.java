package com.apexads.sdk.core.models.openrtb;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class BidResponseTest {

    @Test
    public void getWinningBid_returnsNullWhenSeatBidsMissing() {
        assertThat(new BidResponse().getWinningBid()).isNull();

        BidResponse empty = new BidResponse();
        empty.seatbid = Collections.emptyList();
        assertThat(empty.getWinningBid()).isNull();
    }

    @Test
    public void getWinningBid_ignoresNullBidListsAndReturnsHighestPrice() {
        BidResponse.Bid low = bid(1.25);
        BidResponse.Bid high = bid(4.5);
        BidResponse.SeatBid emptySeat = new BidResponse.SeatBid();
        BidResponse.SeatBid seat = new BidResponse.SeatBid();
        seat.bid = Arrays.asList(low, high);
        BidResponse response = new BidResponse();
        response.seatbid = Arrays.asList(emptySeat, seat);

        assertThat(response.getWinningBid()).isEqualTo(high);
    }

    private static BidResponse.Bid bid(double price) {
        BidResponse.Bid bid = new BidResponse.Bid();
        bid.price = price;
        return bid;
    }
}
