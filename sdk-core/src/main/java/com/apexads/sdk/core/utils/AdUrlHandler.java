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
