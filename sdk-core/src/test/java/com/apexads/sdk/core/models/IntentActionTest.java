package com.apexads.sdk.core.models;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class IntentActionTest {

    @Test
    public void fromJsonParsesWalletActionAndLifecycleTrackers() {
        IntentAction action = IntentAction.fromJson("{" +
                "\"type\":\"save_to_wallet\"," +
                "\"disclosure\":\"Sponsored\"," +
                "\"intent_label\":\"Relevant to your hotel search\"," +
                "\"cta_text\":\"Save to Google Wallet\"," +
                "\"badge_text\":\"Wallet-ready offer\"," +
                "\"started_tracking_url\":\"https://track/start\"}");

        assertThat(action).isNotNull();
        assertThat(action.type).isEqualTo(IntentContext.ActionType.SAVE_TO_WALLET);
        assertThat(action.intentLabel).isEqualTo("Relevant to your hotel search");
        assertThat(action.ctaText).isEqualTo("Save to Google Wallet");
        assertThat(action.startedTrackingUrl).isEqualTo("https://track/start");
    }

    @Test
    public void fromJsonRejectsUnknownOrIncompleteActions() {
        assertThat(IntentAction.fromJson("{\"type\":\"unknown\",\"cta_text\":\"Act\"}"))
                .isNull();
        assertThat(IntentAction.fromJson("{\"type\":\"save_to_wallet\"}"))
                .isNull();
        assertThat(IntentAction.fromJson("not-json")).isNull();
    }
}
