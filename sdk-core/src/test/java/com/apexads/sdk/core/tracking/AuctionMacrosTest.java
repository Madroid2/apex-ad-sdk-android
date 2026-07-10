package com.apexads.sdk.core.tracking;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class AuctionMacrosTest {

    @Test
    public void expand_replacesAllStandardMacros() {
        String url = "https://x.example/win?a=${AUCTION_ID}&i=${AUCTION_IMP_ID}" +
                "&b=${AUCTION_BID_ID}&p=${AUCTION_PRICE}&c=${AUCTION_CURRENCY}";

        String out = AuctionMacros.expand(url, "req-1", "1", "bid-9", 2.5, "USD");

        assertThat(out).isEqualTo(
                "https://x.example/win?a=req-1&i=1&b=bid-9&p=2.5&c=USD");
    }

    @Test
    public void expand_urlEncodesValues() {
        String out = AuctionMacros.expand(
                "https://x.example/win?a=${AUCTION_ID}", "id with space", "1", "b", 1.0, "USD");

        assertThat(out).isEqualTo("https://x.example/win?a=id+with+space");
    }

    @Test
    public void expand_nullValuesBecomeEmpty() {
        String out = AuctionMacros.expand(
                "https://x.example/win?a=${AUCTION_ID}&b=${AUCTION_BID_ID}", null, "1", null, 1.0, "USD");

        assertThat(out).isEqualTo("https://x.example/win?a=&b=");
    }

    @Test
    public void expand_leavesUnknownMacrosUntouched() {
        String url = "https://x.example/win?p=${AUCTION_PRICE}&x=${CUSTOM_MACRO}";

        String out = AuctionMacros.expand(url, "r", "1", "b", 0.5, "USD");

        assertThat(out).isEqualTo("https://x.example/win?p=0.5&x=${CUSTOM_MACRO}");
    }

    @Test
    public void expand_nullOrEmptyUrl_passesThrough() {
        assertThat(AuctionMacros.expand(null, "r", "1", "b", 1.0, "USD")).isNull();
        assertThat(AuctionMacros.expand("", "r", "1", "b", 1.0, "USD")).isEmpty();
    }

    @Test
    public void expandLoss_replacesLossReason() {
        String out = AuctionMacros.expandLoss(
                "https://x.example/loss?r=${AUCTION_LOSS}&p=${AUCTION_PRICE}",
                "req-1", "1", "bid-2", 3.0, "USD",
                AuctionMacros.LOSS_LOST_TO_HIGHER_BID);

        assertThat(out).isEqualTo("https://x.example/loss?r=102&p=3");
    }

    @Test
    public void formatPrice_avoidsScientificNotation() {
        assertThat(AuctionMacros.formatPrice(0.000001)).isEqualTo("0.000001");
        assertThat(AuctionMacros.formatPrice(12.0)).isEqualTo("12");
        assertThat(AuctionMacros.formatPrice(0.0)).isEqualTo("0");
    }
}
