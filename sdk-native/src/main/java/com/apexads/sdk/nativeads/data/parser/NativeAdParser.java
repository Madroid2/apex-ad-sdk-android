package com.apexads.sdk.nativeads;

import androidx.annotation.Nullable;

import com.apexads.sdk.core.models.NativeAdPayload;
import com.apexads.sdk.core.utils.AdLog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses IAB OpenRTB Native 1.2 ad markup JSON into {@link NativeAdPayload}.
 *
 * Uses Android's built-in {@code org.json} — no third-party dependency.
 * Asset IDs match the bid request native request definition sent in
 * {@link com.apexads.sdk.core.request.OpenRTBRequestBuilder}.
 */
final class NativeAdParser {

    private static final int ASSET_TITLE      = 1;
    private static final int ASSET_MAIN_IMAGE = 2;
    private static final int ASSET_ICON       = 3;
    private static final int ASSET_DESC       = 4;
    private static final int ASSET_SPONSOR    = 5;
    private static final int ASSET_CTA        = 6;

    @Nullable
    NativeAdPayload parse(@Nullable String markup) {
        if (markup == null || markup.trim().isEmpty()) return null;
        try {
            JSONObject root    = new JSONObject(markup);
            JSONObject native_ = root.has("native") ? root.getJSONObject("native") : root;

            JSONObject link   = native_.optJSONObject("link");
            String clickUrl   = link != null ? link.optString("url", null) : null;
            if (clickUrl != null && clickUrl.isEmpty()) clickUrl = null;

            List<String> impTrackers = new ArrayList<>();
            JSONArray impArr = native_.optJSONArray("imptrackers");
            if (impArr != null) {
                for (int i = 0; i < impArr.length(); i++) {
                    impTrackers.add(impArr.getString(i));
                }
            }

            String title         = null;
            String description   = null;
            String iconUrl       = null;
            String imageUrl      = null;
            String advertiserName = null;
            String ctaText       = "Learn More";

            JSONArray assets = native_.optJSONArray("assets");
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.getJSONObject(i);
                    int id = asset.getInt("id");
                    switch (id) {
                        case ASSET_TITLE:
                            JSONObject titleObj = asset.optJSONObject("title");
                            if (titleObj != null) title = titleObj.optString("text", null);
                            break;
                        case ASSET_MAIN_IMAGE:
                            JSONObject mainImg = asset.optJSONObject("img");
                            if (mainImg != null) imageUrl = mainImg.optString("url", null);
                            break;
                        case ASSET_ICON:
                            JSONObject iconImg = asset.optJSONObject("img");
                            if (iconImg != null) iconUrl = iconImg.optString("url", null);
                            break;
                        case ASSET_DESC:
                            JSONObject descData = asset.optJSONObject("data");
                            if (descData != null) description = descData.optString("value", null);
                            break;
                        case ASSET_SPONSOR:
                            JSONObject sponsorData = asset.optJSONObject("data");
                            if (sponsorData != null) advertiserName = sponsorData.optString("value", null);
                            break;
                        case ASSET_CTA:
                            JSONObject ctaData = asset.optJSONObject("data");
                            if (ctaData != null) {
                                String v = ctaData.optString("value", null);
                                if (v != null && !v.isEmpty()) ctaText = v;
                            }
                            break;
                    }
                }
            }

            if (title == null) {
                AdLog.w("NativeAdParser: missing required title asset");
                return null;
            }

            return new NativeAdPayload(
                    title,
                    description != null ? description : "",
                    iconUrl,
                    imageUrl,
                    ctaText,
                    advertiserName,
                    clickUrl,
                    Collections.unmodifiableList(impTrackers));

        } catch (Exception e) {
            AdLog.e(e, "NativeAdParser: failed to parse native JSON");
            return null;
        }
    }
}
