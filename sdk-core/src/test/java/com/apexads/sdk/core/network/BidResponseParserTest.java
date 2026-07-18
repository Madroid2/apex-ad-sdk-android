package com.apexads.sdk.core.network;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.apexads.sdk.core.models.openrtb.BidResponse;

import org.json.JSONException;
import org.junit.Test;

public class BidResponseParserTest {

    @Test
    public void parse_fullResponse_mapsTopLevelSeatAndBidFields() throws Exception {
        BidResponse response = BidResponseParser.parse("{"
                + "\"id\":\"req-1\","
                + "\"cur\":\"EUR\","
                + "\"bidid\":\"auction-1\","
                + "\"seatbid\":[{\"seat\":\"dsp-a\",\"group\":1,\"bid\":[{"
                + "\"id\":\"bid-1\","
                + "\"impid\":\"imp-1\","
                + "\"price\":2.75,"
                + "\"adm\":\"<html/>\","
                + "\"nurl\":\"https://win\","
                + "\"burl\":\"https://bill\","
                + "\"lurl\":\"https://loss\","
                + "\"crid\":\"creative-1\","
                + "\"cid\":\"campaign-1\","
                + "\"w\":300,"
                + "\"h\":250,"
                + "\"exp\":60,"
                + "\"api\":6,"
                + "\"protocol\":7,"
                + "\"ext\":{\"wallet\":{\"pass_jwt\":\"jwt\",\"offer_id\":\"offer\"},"
                + "\"apex\":{\"action\":{\"type\":\"save_to_wallet\",\"cta_text\":\"Save to Google Wallet\"}}}"
                + "}]}]}");

        assertThat(response.id).isEqualTo("req-1");
        assertThat(response.cur).isEqualTo("EUR");
        assertThat(response.bidid).isEqualTo("auction-1");
        assertThat(response.seatbid).hasSize(1);
        assertThat(response.seatbid.get(0).seat).isEqualTo("dsp-a");
        assertThat(response.seatbid.get(0).group).isEqualTo(1);

        BidResponse.Bid bid = response.getWinningBid();
        assertThat(bid.id).isEqualTo("bid-1");
        assertThat(bid.impid).isEqualTo("imp-1");
        assertThat(bid.price).isEqualTo(2.75);
        assertThat(bid.adm).isEqualTo("<html/>");
        assertThat(bid.nurl).isEqualTo("https://win");
        assertThat(bid.burl).isEqualTo("https://bill");
        assertThat(bid.lurl).isEqualTo("https://loss");
        assertThat(bid.crid).isEqualTo("creative-1");
        assertThat(bid.cid).isEqualTo("campaign-1");
        assertThat(bid.w).isEqualTo(300);
        assertThat(bid.h).isEqualTo(250);
        assertThat(bid.exp).isEqualTo(60);
        assertThat(bid.api).isEqualTo(6);
        assertThat(bid.protocol).isEqualTo(7);
        assertThat(bid.ext.walletExtJson).contains("\"offer_id\":\"offer\"");
        assertThat(bid.ext.actionExtJson).contains("\"type\":\"save_to_wallet\"");
    }

    @Test
    public void parse_missingOptionalFields_usesDefaultsAndStripsEmptyStrings() throws Exception {
        BidResponse response = BidResponseParser.parse("{"
                + "\"id\":\"req-1\","
                + "\"seatbid\":[{\"bid\":[{\"price\":1.0,\"adm\":\"\",\"nurl\":\"\",\"crid\":\"\"}]}]"
                + "}");

        BidResponse.Bid bid = response.getWinningBid();
        assertThat(response.cur).isEqualTo("USD");
        assertThat(response.nbr).isNull();
        assertThat(bid.adm).isNull();
        assertThat(bid.nurl).isNull();
        assertThat(bid.crid).isNull();
        assertThat(bid.w).isNull();
        assertThat(bid.ext).isNull();
    }

    @Test
    public void parse_noSeatbid_keepsSeatbidNull() throws Exception {
        BidResponse response = BidResponseParser.parse("{\"id\":\"req-1\",\"nbr\":2}");

        assertThat(response.id).isEqualTo("req-1");
        assertThat(response.nbr).isEqualTo(2);
        assertThat(response.seatbid).isNull();
        assertThat(response.getWinningBid()).isNull();
    }

    @Test
    public void parse_malformedJson_throwsJsonException() {
        assertThrows(JSONException.class, () -> BidResponseParser.parse("{"));
    }
}
