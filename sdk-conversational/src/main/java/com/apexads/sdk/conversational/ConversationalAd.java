package com.apexads.sdk.conversational;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.IntentContext;
import com.apexads.sdk.core.presentation.mvvm.AdState;
import com.apexads.sdk.core.presentation.mvvm.AdStateObserver;
import com.apexads.sdk.nativeads.NativeAd;
import com.apexads.sdk.nativeads.NativeAdListener;

/**
 * A Sponsored Suggestion for in-app AI/assistant surfaces.
 *
 * <p>This is the Apex conversational ad format: an ordinary OpenRTB Native 1.2 bid, requested
 * with {@code imp.ext.apex.surface = "assistant"} plus the coarse Intent-to-Action context, and
 * rendered by the publisher as a clearly disclosed card <em>between</em> chat messages. The SDK
 * never reads or transmits conversation content — the only signals sent are the structured
 * {@link IntentContext} taxonomy/category/journey declared by the app — and the loaded creative
 * must never be merged into the assistant's own answer text (answer independence).</p>
 *
 * <pre>{@code
 * val ad = ConversationalAd.Builder("assistant-inline-1")
 *     .intentContext(
 *         IntentContext.builder("apex-commerce-1", "travel.hotel")
 *             .journeyStage(IntentContext.JourneyStage.READY_TO_ACT)
 *             .displayLabel("Relevant to your hotel search")
 *             .supports(IntentContext.ActionType.SAVE_TO_WALLET)
 *             .build())
 *     .listener(listener)
 *     .build()
 * ad.load()
 * // In the chat UI, once ready:
 * val suggestion = ad.getSuggestion()
 * // render suggestion fields; on CTA tap:
 * if (suggestion.hasAction) ad.performAction(context) else ad.handleClick(context)
 * }</pre>
 */
public final class ConversationalAd {

    /** Wire value for a conversational/AI-assistant surface ({@code imp.ext.apex.surface}). */
    public static final String SURFACE_ASSISTANT = "assistant";

    private final NativeAd nativeAd;
    @Nullable private volatile SponsoredSuggestion suggestion;

    private ConversationalAd(@NonNull Builder builder) {
        @Nullable ConversationalAdListener listener = builder.listener;
        nativeAd = new NativeAd.Builder(builder.placementId)
                .intentContext(builder.intentContext)
                .renderSurface(SURFACE_ASSISTANT)
                .listener(new NativeAdListener() {
                    @Override public void onNativeAdLoaded(@NonNull NativeAd ad) {
                        SponsoredSuggestion mapped = SponsoredSuggestion.from(ad);
                        suggestion = mapped;
                        if (listener == null) return;
                        if (mapped != null) {
                            listener.onSuggestionReady(ConversationalAd.this);
                        } else {
                            listener.onSuggestionFailed(
                                    new AdError.NoFill("Bid lacks conversational-renderable assets"));
                        }
                    }

                    @Override public void onNativeAdFailed(@NonNull AdError error) {
                        if (listener != null) listener.onSuggestionFailed(error);
                    }

                    @Override public void onNativeAdClicked() {
                        if (listener != null) listener.onSuggestionClicked();
                    }

                    @Override public void onNativeAdActionCompleted() {
                        if (listener != null) listener.onActionCompleted();
                    }

                    @Override public void onNativeAdActionCancelled() {
                        if (listener != null) listener.onActionCancelled();
                    }

                    @Override public void onNativeAdActionFailed() {
                        if (listener != null) listener.onActionFailed();
                    }
                })
                .build();
    }

    public void load() {
        suggestion = null;
        nativeAd.load();
    }

    /** The compact render contract; null until {@code onSuggestionReady}. */
    @Nullable
    public SponsoredSuggestion getSuggestion() {
        return suggestion;
    }

    public boolean isReady() {
        return suggestion != null && nativeAd.isReady();
    }

    /**
     * Executes the bid's action (e.g. Save to Wallet) with the standard Native
     * click-through as fallback. Wire this to the suggestion's primary CTA.
     */
    public boolean performAction(@NonNull Context context) {
        return nativeAd.performAction(context);
    }

    /** Standard Native 1.2 click-through — the fallback CTA and card tap target. */
    public boolean handleClick(@NonNull Context context) {
        return nativeAd.handleClick(context);
    }

    /**
     * Records the suggestion (and its optional action CTA) as rendered. Call once
     * when the card becomes visible in the conversation.
     */
    public void recordSuggestionRendered() {
        nativeAd.recordActionRendered();
    }

    @NonNull
    public AdState getState() {
        return nativeAd.getState();
    }

    public void addStateObserver(@NonNull AdStateObserver observer) {
        nativeAd.addStateObserver(observer);
    }

    public void removeStateObserver(@NonNull AdStateObserver observer) {
        nativeAd.removeStateObserver(observer);
    }

    public void destroy() {
        nativeAd.destroy();
    }

    public static final class Builder {
        private final String placementId;
        @Nullable private ConversationalAdListener listener;
        @Nullable private IntentContext intentContext;

        public Builder(@Nullable String placementId) { this.placementId = placementId; }

        public Builder listener(@NonNull ConversationalAdListener l) { listener = l; return this; }

        /**
         * Required. Only structured taxonomy/category data may be supplied — never raw
         * chat messages, prompts, or other free-form conversation content.
         */
        public Builder intentContext(@NonNull IntentContext context) {
            intentContext = context;
            return this;
        }

        @NonNull
        public ConversationalAd build() {
            if (intentContext == null) {
                throw new IllegalStateException(
                        "ConversationalAd requires an IntentContext: declare the coarse journey "
                                + "taxonomy/category for this assistant surface.");
            }
            return new ConversationalAd(this);
        }
    }
}
