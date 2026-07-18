package com.apexads.sdk.conversational;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.nativeads.NativeAd;

/**
 * The compact render contract of a conversational Sponsored Suggestion.
 *
 * <p>A suggestion reuses the ordinary OpenRTB Native 1.2 assets of the bid, re-flowed for a
 * chat/assistant surface: the title and body are clamped to conversational lengths, the main
 * image becomes a thumbnail, and the disclosure label is always present. Publishers render
 * these fields inside their own chat UI; the card must stay visually distinct from organic
 * assistant messages and must never be inserted into the assistant's own answer text.</p>
 */
public final class SponsoredSuggestion {

    /** Maximum rendered title length on a conversational surface. */
    public static final int MAX_TITLE_LENGTH = 60;
    /** Maximum rendered body length on a conversational surface. */
    public static final int MAX_BODY_LENGTH = 90;

    @NonNull  public final String title;
    @NonNull  public final String body;
    @Nullable public final String advertiserName;
    @Nullable public final String iconUrl;
    /** Native 1.2 main image, rendered as a compact thumbnail on this surface. */
    @Nullable public final String thumbnailUrl;
    /** Always non-empty; defaults to "Sponsored". Render it in the card header. */
    @NonNull  public final String disclosure;
    /** Optional consent-transparency chip, e.g. "Relevant to your hotel search". */
    @Nullable public final String relevanceLabel;
    /** CTA for the executable action; null when demand returned no action. */
    @Nullable public final String actionCtaText;
    /** Standard Native click-through CTA — the always-available fallback. */
    @NonNull  public final String fallbackCtaText;
    @Nullable public final String badgeText;
    public final boolean hasAction;

    private SponsoredSuggestion(@NonNull String title,
                                @NonNull String body,
                                @Nullable String advertiserName,
                                @Nullable String iconUrl,
                                @Nullable String thumbnailUrl,
                                @NonNull String disclosure,
                                @Nullable String relevanceLabel,
                                @Nullable String actionCtaText,
                                @NonNull String fallbackCtaText,
                                @Nullable String badgeText,
                                boolean hasAction) {
        this.title = title;
        this.body = body;
        this.advertiserName = advertiserName;
        this.iconUrl = iconUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.disclosure = disclosure;
        this.relevanceLabel = relevanceLabel;
        this.actionCtaText = actionCtaText;
        this.fallbackCtaText = fallbackCtaText;
        this.badgeText = badgeText;
        this.hasAction = hasAction;
    }

    /** Maps a loaded {@link NativeAd} onto the conversational render contract. */
    @Nullable
    static SponsoredSuggestion from(@NonNull NativeAd ad) {
        return fromAssets(
                ad.getTitle(),
                ad.getDescription(),
                ad.getAdvertiserName(),
                ad.getIconUrl(),
                ad.getImageUrl(),
                ad.getDisclosureText(),
                ad.getIntentLabel(),
                ad.getActionCtaText(),
                ad.getCtaText(),
                ad.getActionBadgeText(),
                ad.hasIntentAction());
    }

    /** Pure mapping from raw Native 1.2 + action assets; null when unrenderable. */
    @Nullable
    static SponsoredSuggestion fromAssets(@Nullable String title,
                                          @Nullable String description,
                                          @Nullable String advertiserName,
                                          @Nullable String iconUrl,
                                          @Nullable String imageUrl,
                                          @Nullable String disclosure,
                                          @Nullable String relevanceLabel,
                                          @Nullable String actionCtaText,
                                          @Nullable String fallbackCtaText,
                                          @Nullable String badgeText,
                                          boolean hasAction) {
        if (title == null || title.trim().isEmpty()) return null;
        return new SponsoredSuggestion(
                clamp(title.trim(), MAX_TITLE_LENGTH),
                clamp(description == null ? "" : description.trim(), MAX_BODY_LENGTH),
                advertiserName,
                iconUrl,
                imageUrl,
                disclosure == null || disclosure.trim().isEmpty() ? "Sponsored" : disclosure.trim(),
                relevanceLabel,
                actionCtaText,
                fallbackCtaText == null || fallbackCtaText.trim().isEmpty()
                        ? "Learn more" : fallbackCtaText.trim(),
                badgeText,
                hasAction);
    }

    /**
     * Clamps text to {@code max} characters, cutting at the last word boundary and
     * appending an ellipsis. Text at or under the limit is returned unchanged.
     */
    @NonNull
    static String clamp(@NonNull String text, int max) {
        if (text.length() <= max) return text;
        String cut = text.substring(0, max - 1);
        int lastSpace = cut.lastIndexOf(' ');
        if (lastSpace > max / 2) {
            cut = cut.substring(0, lastSpace);
        }
        return cut + "…";
    }
}
