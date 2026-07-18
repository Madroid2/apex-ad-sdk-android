package com.apexads.sdk.conversational;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.error.AdError;

public interface ConversationalAdListener {
    void onSuggestionReady(@NonNull ConversationalAd ad);
    void onSuggestionFailed(@NonNull AdError error);
    default void onSuggestionClicked() {}
    default void onActionCompleted() {}
    default void onActionCancelled() {}
    default void onActionFailed() {}
}
