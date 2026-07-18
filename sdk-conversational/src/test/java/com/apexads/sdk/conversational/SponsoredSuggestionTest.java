package com.apexads.sdk.conversational;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class SponsoredSuggestionTest {

    @Test
    public void fromAssets_mapsFullCardContract() {
        SponsoredSuggestion suggestion = SponsoredSuggestion.fromAssets(
                "Save ₹1,500 on your Bengaluru stay",
                "Valid until 30 Sep. Save this offer now and use it when you book.",
                "StayVista",
                "https://cdn.example/icon.png",
                "https://cdn.example/hero.jpg",
                "Sponsored",
                "Relevant to your hotel search",
                "Save to Google Wallet",
                "View hotel",
                "Wallet-ready offer",
                true);

        assertThat(suggestion).isNotNull();
        assertThat(suggestion.title).isEqualTo("Save ₹1,500 on your Bengaluru stay");
        assertThat(suggestion.body).startsWith("Valid until 30 Sep.");
        assertThat(suggestion.advertiserName).isEqualTo("StayVista");
        assertThat(suggestion.thumbnailUrl).isEqualTo("https://cdn.example/hero.jpg");
        assertThat(suggestion.disclosure).isEqualTo("Sponsored");
        assertThat(suggestion.relevanceLabel).isEqualTo("Relevant to your hotel search");
        assertThat(suggestion.actionCtaText).isEqualTo("Save to Google Wallet");
        assertThat(suggestion.fallbackCtaText).isEqualTo("View hotel");
        assertThat(suggestion.badgeText).isEqualTo("Wallet-ready offer");
        assertThat(suggestion.hasAction).isTrue();
    }

    @Test
    public void fromAssets_withoutTitle_isUnrenderable() {
        assertThat(SponsoredSuggestion.fromAssets(
                null, "body", "adv", null, null, null, null, null, null, null, false)).isNull();
        assertThat(SponsoredSuggestion.fromAssets(
                "  ", "body", "adv", null, null, null, null, null, null, null, false)).isNull();
    }

    @Test
    public void fromAssets_appliesConversationalDefaults() {
        SponsoredSuggestion suggestion = SponsoredSuggestion.fromAssets(
                "Title", null, null, null, null, "  ", null, null, "  ", null, false);

        assertThat(suggestion).isNotNull();
        assertThat(suggestion.body).isEmpty();
        assertThat(suggestion.disclosure).isEqualTo("Sponsored");
        assertThat(suggestion.fallbackCtaText).isEqualTo("Learn more");
        assertThat(suggestion.actionCtaText).isNull();
        assertThat(suggestion.hasAction).isFalse();
    }

    @Test
    public void clamp_truncatesAtWordBoundaryWithEllipsis() {
        String longTitle = "An exceptionally long sponsored headline that would overflow the "
                + "conversational card if rendered untouched";

        String clamped = SponsoredSuggestion.clamp(longTitle, SponsoredSuggestion.MAX_TITLE_LENGTH);

        assertThat(clamped.length()).isAtMost(SponsoredSuggestion.MAX_TITLE_LENGTH);
        assertThat(clamped).endsWith("…");
        assertThat(clamped).doesNotContain("overflo…");
    }

    @Test
    public void clamp_leavesShortTextUntouched() {
        assertThat(SponsoredSuggestion.clamp("Short title", 60)).isEqualTo("Short title");
    }

    @Test
    public void clamp_handlesUnbrokenTextWithoutSpaces() {
        String unbroken = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

        String clamped = SponsoredSuggestion.clamp(unbroken, 60);

        assertThat(clamped.length()).isEqualTo(60);
        assertThat(clamped).endsWith("…");
    }
}
