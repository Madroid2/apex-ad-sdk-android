package com.apexads.sdk.core.models;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.util.Map;

public class IntentContextTest {

    @Test
    public void builderCreatesCoarseOpenRtbContext() {
        IntentContext context = IntentContext.builder("apex-commerce-1", "travel.hotel")
                .journeyStage(IntentContext.JourneyStage.READY_TO_ACT)
                .displayLabel("Relevant to your hotel search")
                .supports(IntentContext.ActionType.SAVE_TO_WALLET)
                .supports(IntentContext.ActionType.SAVE_TO_WALLET)
                .build();

        Map<String, Object> map = context.toOpenRtbMap();
        assertThat(map.get("taxonomy")).isEqualTo("apex-commerce-1");
        assertThat(map.get("category")).isEqualTo("travel.hotel");
        assertThat(map.get("journey_stage")).isEqualTo("ready_to_act");
        assertThat(map.get("label")).isEqualTo("Relevant to your hotel search");
        assertThat(context.supportedActions).containsExactly(IntentContext.ActionType.SAVE_TO_WALLET);
    }

    @Test
    public void builderRejectsMissingStructuredCategory() {
        assertThrows(IllegalArgumentException.class,
                () -> IntentContext.builder("apex-commerce-1", "  "));
    }
}
