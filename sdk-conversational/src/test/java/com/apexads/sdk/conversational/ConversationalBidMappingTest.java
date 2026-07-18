package com.apexads.sdk.conversational;

import static com.google.common.truth.Truth.assertThat;

import com.apexads.sdk.core.models.IntentAction;
import com.apexads.sdk.core.models.IntentContext;

import org.junit.Test;

/**
 * Integration-level mapping test: a realistic {@code bid.ext.apex.action} payload
 * (as returned by apex-demand-platform for an assistant-surface Native bid) flows
 * through {@link IntentAction} parsing into the conversational render contract.
 */
public class ConversationalBidMappingTest {

    private static final String ACTION_EXT_JSON = "{"
            + "\"type\":\"save_to_wallet\","
            + "\"disclosure\":\"Sponsored\","
            + "\"intent_label\":\"Relevant to your hotel search\","
            + "\"cta_text\":\"Save to Google Wallet\","
            + "\"badge_text\":\"Wallet-ready offer\","
            + "\"rendered_tracking_url\":\"https://dsp.example/track/wallet_cta_rendered?sig=abc\""
            + "}";

    @Test
    public void demandActionExt_flowsIntoSuggestionContract() {
        IntentAction action = IntentAction.fromJson(ACTION_EXT_JSON);
        assertThat(action).isNotNull();
        assertThat(action.type).isEqualTo(IntentContext.ActionType.SAVE_TO_WALLET);

        SponsoredSuggestion suggestion = SponsoredSuggestion.fromAssets(
                "Save ₹1,500 on your Bengaluru stay",
                "Valid until 30 September. Save this exclusive offer now and use it when you book your next weekend stay.",
                "StayVista",
                "https://cdn.example/icon.png",
                "https://cdn.example/hero.jpg",
                action.disclosure,
                action.intentLabel,
                action.ctaText,
                "View hotel",
                action.badgeText,
                true);

        assertThat(suggestion).isNotNull();
        assertThat(suggestion.disclosure).isEqualTo("Sponsored");
        assertThat(suggestion.relevanceLabel).isEqualTo("Relevant to your hotel search");
        assertThat(suggestion.actionCtaText).isEqualTo("Save to Google Wallet");
        assertThat(suggestion.hasAction).isTrue();
        assertThat(suggestion.body.length()).isAtMost(SponsoredSuggestion.MAX_BODY_LENGTH);
        assertThat(suggestion.body).endsWith("…");
    }

    @Test
    public void demandBidWithoutAction_staysStandardNativeSuggestion() {
        IntentAction action = IntentAction.fromJson(null);
        assertThat(action).isNull();

        SponsoredSuggestion suggestion = SponsoredSuggestion.fromAssets(
                "Weekend escapes from ₹2,999",
                "Handpicked stays across Karnataka.",
                "StayVista",
                null,
                null,
                null,
                null,
                null,
                "View stays",
                null,
                false);

        assertThat(suggestion).isNotNull();
        assertThat(suggestion.hasAction).isFalse();
        assertThat(suggestion.actionCtaText).isNull();
        assertThat(suggestion.fallbackCtaText).isEqualTo("View stays");
        assertThat(suggestion.disclosure).isEqualTo("Sponsored");
    }
}
