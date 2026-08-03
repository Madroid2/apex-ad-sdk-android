package com.apexads.sdk.core.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * How the server says this placement should be presented.
 *
 * <p>The policy arrives on the bid response ({@code ext.apex.render}) rather than from a
 * separate config endpoint, so it costs no extra round trip and nothing is added to the
 * cold-start path: the instructions arrive with the ad they apply to.</p>
 *
 * <p>Every value is clamped again here. The server already bounds them, but this is a
 * loop that runs on someone's battery, so the SDK does not take a remote number on
 * trust — a hostile or misconfigured response cannot make the slot spin.</p>
 */
public final class RenderPolicy {

    /** Refresh floor enforced on-device regardless of what the server sends. */
    public static final int MIN_REFRESH_SECONDS = 15;
    public static final int MAX_REFRESH_SECONDS = 600;
    private static final int MAX_DIMENSION_DP = 4000;
    private static final int MAX_CLOSE_SECONDS = 120;
    private static final int MAX_PER_SESSION = 100;

    /** The policy applied when the server sends none: render once, no refresh. */
    public static final RenderPolicy DEFAULT = new RenderPolicy(0, 0, 0, 0, false, 0);

    /** Seconds between automatic reloads of an inline slot; 0 disables refresh. */
    public final int refreshSeconds;
    /** Requested render size in dp; 0 means size to the container. */
    public final int widthDp;
    public final int heightDp;
    /** Seconds before a fullscreen ad self-dismisses; 0 leaves it to the user. */
    public final int closeAfterSeconds;
    /** Whether video creatives start muted. */
    public final boolean muted;
    /** Maximum renders of this placement per app session; 0 is unlimited. */
    public final int maxPerSession;

    private RenderPolicy(int refreshSeconds, int widthDp, int heightDp,
                         int closeAfterSeconds, boolean muted, int maxPerSession) {
        this.refreshSeconds = refreshSeconds;
        this.widthDp = widthDp;
        this.heightDp = heightDp;
        this.closeAfterSeconds = closeAfterSeconds;
        this.muted = muted;
        this.maxPerSession = maxPerSession;
    }

    /**
     * Parses {@code ext.apex.render} from a bid response. Returns {@link #DEFAULT} when the
     * response carries no policy, so an unconfigured placement behaves exactly as before.
     */
    @NonNull
    public static RenderPolicy fromResponseExt(@Nullable String extJson) {
        if (extJson == null || extJson.isEmpty()) return DEFAULT;
        try {
            JSONObject apex = new JSONObject(extJson).optJSONObject("apex");
            if (apex == null) return DEFAULT;
            JSONObject render = apex.optJSONObject("render");
            if (render == null) return DEFAULT;
            return new RenderPolicy(
                    clampRefresh(render.optInt("refresh_seconds", 0)),
                    clamp(render.optInt("width_dp", 0), 0, MAX_DIMENSION_DP),
                    clamp(render.optInt("height_dp", 0), 0, MAX_DIMENSION_DP),
                    clamp(render.optInt("close_after_seconds", 0), 0, MAX_CLOSE_SECONDS),
                    render.optBoolean("muted", false),
                    clamp(render.optInt("max_per_session", 0), 0, MAX_PER_SESSION));
        } catch (JSONException ignored) {
            return DEFAULT;
        }
    }

    /** True when a refresh interval was requested and survived clamping. */
    public boolean refreshes() {
        return refreshSeconds >= MIN_REFRESH_SECONDS;
    }

    /**
     * A refresh below the floor is treated as "off" rather than raised to the floor:
     * a server asking for a 1-second refresh is misconfigured, and quietly running it
     * at 15s would hide that while still costing the user battery.
     */
    private static int clampRefresh(int seconds) {
        if (seconds <= 0) return 0;
        if (seconds < MIN_REFRESH_SECONDS || seconds > MAX_REFRESH_SECONDS) return 0;
        return seconds;
    }

    private static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }
}
