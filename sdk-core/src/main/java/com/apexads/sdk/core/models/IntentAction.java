package com.apexads.sdk.core.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/** Optional action metadata returned alongside a standard native bid. */
public final class IntentAction {

    @NonNull public final IntentContext.ActionType type;
    @NonNull public final String disclosure;
    @Nullable public final String intentLabel;
    @NonNull public final String ctaText;
    @Nullable public final String badgeText;
    @Nullable public final String renderedTrackingUrl;
    @Nullable public final String startedTrackingUrl;
    @Nullable public final String cancelledTrackingUrl;
    @Nullable public final String failedTrackingUrl;

    private IntentAction(
            @NonNull IntentContext.ActionType type,
            @NonNull String disclosure,
            @Nullable String intentLabel,
            @NonNull String ctaText,
            @Nullable String badgeText,
            @Nullable String renderedTrackingUrl,
            @Nullable String startedTrackingUrl,
            @Nullable String cancelledTrackingUrl,
            @Nullable String failedTrackingUrl) {
        this.type = type;
        this.disclosure = disclosure;
        this.intentLabel = intentLabel;
        this.ctaText = ctaText;
        this.badgeText = badgeText;
        this.renderedTrackingUrl = renderedTrackingUrl;
        this.startedTrackingUrl = startedTrackingUrl;
        this.cancelledTrackingUrl = cancelledTrackingUrl;
        this.failedTrackingUrl = failedTrackingUrl;
    }

    @Nullable
    public static IntentAction fromJson(@Nullable String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            JSONObject o = new JSONObject(json);
            String wireType = o.optString("type", "");
            IntentContext.ActionType type;
            if (IntentContext.ActionType.SAVE_TO_WALLET.wireValue().equals(wireType)) {
                type = IntentContext.ActionType.SAVE_TO_WALLET;
            } else if (IntentContext.ActionType.OPEN_DEEPLINK.wireValue().equals(wireType)) {
                type = IntentContext.ActionType.OPEN_DEEPLINK;
            } else {
                return null;
            }
            String cta = normalized(o.optString("cta_text", null));
            if (cta == null) return null;
            String disclosure = normalized(o.optString("disclosure", null));
            return new IntentAction(
                    type,
                    disclosure != null ? disclosure : "Sponsored",
                    normalized(o.optString("intent_label", null)),
                    cta,
                    normalized(o.optString("badge_text", null)),
                    normalized(o.optString("rendered_tracking_url", null)),
                    normalized(o.optString("started_tracking_url", null)),
                    normalized(o.optString("cancelled_tracking_url", null)),
                    normalized(o.optString("failed_tracking_url", null)));
        } catch (JSONException ignored) {
            return null;
        }
    }

    @Nullable
    private static String normalized(@Nullable String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
