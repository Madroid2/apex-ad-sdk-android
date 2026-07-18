package com.apexads.sdk.core.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Coarse, publisher-declared context for an Intent-to-Action native placement.
 *
 * <p>This object is deliberately structured: publishers should map an in-app journey to a
 * taxonomy/category and must not pass raw search text, chat messages, or other free-form user
 * content. The bid still uses an ordinary OpenRTB Native 1.2 creative; this context only lets
 * eligible demand return an optional, executable action.</p>
 */
public final class IntentContext {

    public enum JourneyStage {
        DISCOVERY("discovery"),
        CONSIDERATION("consideration"),
        READY_TO_ACT("ready_to_act");

        @NonNull private final String wireValue;

        JourneyStage(@NonNull String wireValue) { this.wireValue = wireValue; }

        @NonNull public String wireValue() { return wireValue; }
    }

    public enum ActionType {
        SAVE_TO_WALLET("save_to_wallet"),
        OPEN_DEEPLINK("open_deeplink");

        @NonNull private final String wireValue;

        ActionType(@NonNull String wireValue) { this.wireValue = wireValue; }

        @NonNull public String wireValue() { return wireValue; }
    }

    @NonNull public final String taxonomy;
    @NonNull public final String category;
    @NonNull public final JourneyStage journeyStage;
    @Nullable public final String displayLabel;
    @NonNull public final List<ActionType> supportedActions;

    private IntentContext(@NonNull Builder builder) {
        taxonomy = builder.taxonomy;
        category = builder.category;
        journeyStage = builder.journeyStage;
        displayLabel = builder.displayLabel;
        supportedActions = Collections.unmodifiableList(new ArrayList<>(builder.supportedActions));
    }

    /** Builds the nested {@code imp.ext.apex.intent} object. */
    @NonNull
    public Map<String, Object> toOpenRtbMap() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("taxonomy", taxonomy);
        value.put("category", category);
        value.put("journey_stage", journeyStage.wireValue());
        if (displayLabel != null) value.put("label", displayLabel);
        return value;
    }

    @NonNull
    public static Builder builder(@NonNull String taxonomy, @NonNull String category) {
        return new Builder(taxonomy, category);
    }

    public static final class Builder {
        @NonNull private final String taxonomy;
        @NonNull private final String category;
        @NonNull private JourneyStage journeyStage = JourneyStage.CONSIDERATION;
        @Nullable private String displayLabel;
        @NonNull private final List<ActionType> supportedActions = new ArrayList<>();

        private Builder(@NonNull String taxonomy, @NonNull String category) {
            if (taxonomy.trim().isEmpty()) throw new IllegalArgumentException("taxonomy must not be empty");
            if (category.trim().isEmpty()) throw new IllegalArgumentException("category must not be empty");
            this.taxonomy = taxonomy.trim();
            this.category = category.trim();
        }

        @NonNull
        public Builder journeyStage(@NonNull JourneyStage value) {
            journeyStage = value;
            return this;
        }

        /** A short disclosure label, for example "Relevant to your hotel search". */
        @NonNull
        public Builder displayLabel(@Nullable String value) {
            String normalized = value == null ? null : value.trim();
            displayLabel = normalized == null || normalized.isEmpty() ? null : normalized;
            return this;
        }

        @NonNull
        public Builder supports(@NonNull ActionType value) {
            if (!supportedActions.contains(value)) supportedActions.add(value);
            return this;
        }

        @NonNull
        public IntentContext build() { return new IntentContext(this); }
    }
}
