package com.apexads.sdk.core.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.BuildConfig;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class AdUrlHandler {

    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";
    private static final String SCHEME_MARKET = "market";
    private static final String PLAY_STORE_WEB_BASE = "https://play.google.com/store/apps/details";

    /**
     * Schemes that must never be launched from ad content. {@code intent://} can
     * target arbitrary (incl. non-exported-intent-filter) components with extras,
     * {@code javascript:}/{@code data:}/{@code blob:} execute content, and
     * {@code file:}/{@code content:} read local data.
     */
    private static final java.util.Set<String> BLOCKED_DEEPLINK_SCHEMES = new java.util.HashSet<>(
            java.util.Arrays.asList("intent", "javascript", "file", "content", "data",
                    "about", "blob", "vbscript", "android-app"));

    /** Result of {@link #openClickThrough}: nothing could be opened. */
    public static final int OPEN_FAILED = 0;
    /** The primary URL opened as a normal web click-through. */
    public static final int OPENED_WEB = 1;
    /** The primary URL opened as an app deep link (custom scheme or market://). */
    public static final int OPENED_DEEPLINK = 2;
    /** A market:// link had no handler; the Play Store web page opened instead. */
    public static final int OPENED_MARKET_WEB = 3;
    /** The primary destination failed; the fallback web URL opened. */
    public static final int OPENED_FALLBACK = 4;

    // Local/private hosts are blocked (SSRF / local-network protection) except in
    // debug builds, where the local demand platform serves click-tracking
    // redirects from a private LAN IP. Package-visible so tests can pin the
    // production-strict path regardless of the build variant they run under.
    static boolean allowLocalHosts = BuildConfig.DEBUG;

    private AdUrlHandler() {}

    public static boolean openExternalUrl(@NonNull Context context,
                                          @Nullable String rawUrl,
                                          @NonNull String source) {
        String safeUrl = normalizeExternalWebUrl(rawUrl);
        if (safeUrl == null) {
            AdLog.w("%s: blocked unsafe click URL: %s", source, redactForLog(rawUrl));
            return false;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            AdLog.w(e, "%s: no handler for click URL: %s", source, safeUrl);
        } catch (Exception e) {
            AdLog.w(e, "%s: could not open click URL: %s", source, safeUrl);
        }
        return false;
    }

    public static boolean openValidatedExternalUrl(@NonNull Context context,
                                                   @NonNull String safeUrl,
                                                   @NonNull String source) {
        String normalized = normalizeExternalWebUrl(safeUrl);
        if (normalized == null) {
            AdLog.w("%s: blocked unsafe click URL: %s", source, redactForLog(safeUrl));
            return false;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(normalized));
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            AdLog.w(e, "%s: no handler for click URL: %s", source, normalized);
        } catch (Exception e) {
            AdLog.w(e, "%s: could not open click URL: %s", source, normalized);
        }
        return false;
    }

    /**
     * Deep-link-aware click-through. Resolution order:
     * <ol>
     *   <li>{@code primaryUrl} is a safe web URL → open it (normal click-through).</li>
     *   <li>{@code primaryUrl} is a safe app deep link (custom scheme or
     *       {@code market://}) → launch it directly.</li>
     *   <li>An unhandled {@code market://} link → open the Play Store web page.</li>
     *   <li>Anything else → open {@code fallbackUrl} (the advertiser's web landing).</li>
     * </ol>
     * The return value says which path opened so callers can decide whether to
     * fire click trackers themselves (deep links bypass server-side click
     * redirects, so {@link #OPENED_DEEPLINK}/{@link #OPENED_MARKET_WEB} clicks
     * must be tracked client-side).
     */
    public static int openClickThrough(@NonNull Context context,
                                       @Nullable String primaryUrl,
                                       @Nullable String fallbackUrl,
                                       @NonNull String source) {
        String web = normalizeExternalWebUrl(primaryUrl);
        if (web != null) {
            if (openValidatedExternalUrl(context, web, source)) return OPENED_WEB;
            return openFallback(context, fallbackUrl, source);
        }

        String deeplink = normalizeDeeplink(primaryUrl);
        if (deeplink != null) {
            if (launchDeeplink(context, deeplink, source)) return OPENED_DEEPLINK;
            String marketWeb = marketToPlayStoreWebUrl(deeplink);
            if (marketWeb != null && openExternalUrl(context, marketWeb, source)) {
                return OPENED_MARKET_WEB;
            }
            AdLog.w("%s: no handler for deep link: %s", source, redactForLog(deeplink));
        } else if (primaryUrl != null) {
            AdLog.w("%s: blocked unsafe click-through URL: %s", source, redactForLog(primaryUrl));
        }
        return openFallback(context, fallbackUrl, source);
    }

    private static int openFallback(@NonNull Context context,
                                    @Nullable String fallbackUrl,
                                    @NonNull String source) {
        if (fallbackUrl != null && openExternalUrl(context, fallbackUrl, source + ".fallback")) {
            return OPENED_FALLBACK;
        }
        return OPEN_FAILED;
    }

    /**
     * Validates an app deep link (e.g. {@code myapp://product/42} or
     * {@code market://details?id=com.example}). Returns null for web URLs (those
     * take the {@link #normalizeExternalWebUrl} path), for the blocked scheme
     * list, and for anything malformed. Unlike web URLs there is no host
     * allow/deny logic — the URI never reaches the network; it only resolves to
     * an installed app's BROWSABLE intent filter.
     */
    @Nullable
    public static String normalizeDeeplink(@Nullable String rawUrl) {
        if (rawUrl == null) return null;

        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty() || containsControlCharacter(trimmed)) return null;

        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException ignored) {
            return null;
        }

        String scheme = uri.getScheme();
        if (scheme == null) return null;

        String normalizedScheme = scheme.toLowerCase(Locale.US);
        if (SCHEME_HTTP.equals(normalizedScheme) || SCHEME_HTTPS.equals(normalizedScheme)) {
            return null; // web URL, not a deep link
        }
        if (BLOCKED_DEEPLINK_SCHEMES.contains(normalizedScheme)) {
            return null;
        }
        String schemeSpecific = uri.getSchemeSpecificPart();
        if (schemeSpecific == null || schemeSpecific.isEmpty() || "//".equals(schemeSpecific)) {
            return null; // no destination
        }
        return uri.toASCIIString();
    }

    /**
     * {@code market://…} → the equivalent {@code https://play.google.com/store/apps/…}
     * page, for devices without a Play Store handler. Returns null for
     * non-market links or a market link without a query.
     */
    @Nullable
    public static String marketToPlayStoreWebUrl(@Nullable String deeplink) {
        if (deeplink == null) return null;
        URI uri;
        try {
            uri = new URI(deeplink);
        } catch (URISyntaxException ignored) {
            return null;
        }
        if (!SCHEME_MARKET.equalsIgnoreCase(uri.getScheme())) return null;
        String query = uri.getRawQuery();
        if (query == null || query.isEmpty()) return null;
        return PLAY_STORE_WEB_BASE + "?" + query;
    }

    private static boolean launchDeeplink(@NonNull Context context,
                                          @NonNull String deeplink,
                                          @NonNull String source) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(deeplink));
            // BROWSABLE restricts resolution to intent filters that opted into
            // being reachable from web/ad content — the same contract browsers
            // enforce for deep links. Never launch non-browsable components.
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            AdLog.d("%s: deep link has no handler: %s", source, redactForLog(deeplink));
        } catch (Exception e) {
            AdLog.w(e, "%s: could not launch deep link: %s", source, redactForLog(deeplink));
        }
        return false;
    }

    @Nullable
    public static String normalizeExternalWebUrl(@Nullable String rawUrl) {
        if (rawUrl == null) return null;

        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty() || containsControlCharacter(trimmed)) return null;

        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException ignored) {
            return null;
        }

        String scheme = uri.getScheme();
        if (scheme == null) return null;

        String normalizedScheme = scheme.toLowerCase(Locale.US);
        if (!SCHEME_HTTPS.equals(normalizedScheme) && !SCHEME_HTTP.equals(normalizedScheme)) {
            return null;
        }

        if (uri.isOpaque() || uri.getHost() == null || uri.getHost().isEmpty()) {
            return null;
        }
        // Reject local/private hosts (SSRF / local-network protection). Exempt in
        // debug (see allowLocalHosts): the local demand platform serves click
        // redirects from a private LAN IP (e.g. 192.168.x:3000), so blocking it
        // there would make every ad's CTA dead. Enforced in release builds.
        if (isLocalOrPrivateHost(uri.getHost()) && !allowLocalHosts) {
            return null;
        }

        return uri.toASCIIString();
    }

    private static boolean isLocalOrPrivateHost(@NonNull String rawHost) {
        String host = rawHost.toLowerCase(Locale.US);
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        while (host.endsWith(".")) {
            host = host.substring(0, host.length() - 1);
        }

        if ("localhost".equals(host) || host.endsWith(".localhost") || ".local".equals(host)
                || host.endsWith(".local")) {
            return true;
        }

        if (host.indexOf(':') >= 0) {
            return isPrivateIpv6Literal(host);
        }

        if (looksLikeNumericIpv4Host(host)) {
            return parseIpv4Octets(host) == null || isPrivateIpv4Literal(host);
        }

        return false;
    }

    private static boolean isPrivateIpv4Literal(@NonNull String host) {
        int[] octets = parseIpv4Octets(host);
        if (octets == null) return false;

        return octets[0] == 0
                || octets[0] == 10
                || octets[0] == 127
                || (octets[0] == 169 && octets[1] == 254)
                || (octets[0] == 172 && octets[1] >= 16 && octets[1] <= 31)
                || (octets[0] == 192 && octets[1] == 168)
                || (octets[0] == 100 && octets[1] >= 64 && octets[1] <= 127)
                || (octets[0] == 198 && (octets[1] == 18 || octets[1] == 19));
    }

    @Nullable
    private static int[] parseIpv4Octets(@NonNull String host) {
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4) return null;

        int[] octets = new int[4];
        for (int i = 0; i < parts.length; i++) {
            try {
                if (parts[i].isEmpty() || parts[i].length() > 3) return null;
                if (parts[i].length() > 1 && parts[i].startsWith("0")) return null;
                octets[i] = Integer.parseInt(parts[i]);
                if (octets[i] < 0 || octets[i] > 255) return null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return octets;
    }

    private static boolean looksLikeNumericIpv4Host(@NonNull String host) {
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if ((c < '0' || c > '9') && c != '.') {
                return false;
            }
        }
        return true;
    }

    private static boolean isPrivateIpv6Literal(@NonNull String host) {
        return "::1".equals(host)
                || host.startsWith("fc")
                || host.startsWith("fd")
                || host.startsWith("fe80:")
                || host.startsWith("::ffff:");
    }

    private static boolean containsControlCharacter(@NonNull String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private static String redactForLog(@Nullable String rawUrl) {
        if (rawUrl == null) return "<null>";
        String trimmed = rawUrl.trim().replaceAll("\\p{Cntrl}", "?");
        return trimmed.length() > 96 ? trimmed.substring(0, 96) + "..." : trimmed;
    }
}
