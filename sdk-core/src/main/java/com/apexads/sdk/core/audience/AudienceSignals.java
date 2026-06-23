package com.apexads.sdk.core.audience;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.device.DeviceInfoProvider;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * An immutable snapshot of the cheap, already-collected signals the on-device cohort
 * engine is allowed to match against.
 *
 * <p>Deliberately constrained: it is built <em>only</em> from data the SDK already gathers
 * for the bid request ({@link DeviceInfoProvider.DeviceInfo}). No sensors, no location
 * polling, no installed-app scanning, no new permissions. This keeps cohort evaluation a
 * handful of in-memory comparisons rather than the resource drain that on-device ML and
 * sensor collection caused in earlier audience SDKs.</p>
 */
public final class AudienceSignals {

    // Canonical field names referenced by declarative rules.
    public static final String FIELD_LANGUAGE = "language";
    public static final String FIELD_COUNTRY = "country";
    public static final String FIELD_DEVICE_TYPE = "deviceType";
    public static final String FIELD_OS = "os";
    public static final String FIELD_BUNDLE = "bundle";
    public static final String FIELD_MANUFACTURER = "manufacturer";
    public static final String FIELD_MODEL = "model";
    public static final String FIELD_CONNECTION_TYPE = "connectionType";

    public static final String DEVICE_TYPE_PHONE = "phone";
    public static final String DEVICE_TYPE_TABLET = "tablet";

    private final Map<String, String> strings;
    private final Map<String, Double> numbers;

    private AudienceSignals(Map<String, String> strings, Map<String, Double> numbers) {
        this.strings = strings;
        this.numbers = numbers;
    }

    @Nullable
    public String getString(@NonNull String field) {
        return strings.get(field);
    }

    @Nullable
    public Double getNumber(@NonNull String field) {
        return numbers.get(field);
    }

    /**
     * Builds a signal snapshot from the device info already assembled for the bid request.
     */
    @NonNull
    public static AudienceSignals from(@NonNull DeviceInfoProvider.DeviceInfo device) {
        Map<String, String> strings = new HashMap<>();
        Map<String, Double> numbers = new HashMap<>();

        String language = device.language;
        if (language != null) {
            // language tag like "en-US" → language "en", country "US"
            String lower = language.toLowerCase(Locale.ROOT);
            String[] parts = lower.split("[-_]");
            strings.put(FIELD_LANGUAGE, parts[0]);
            if (parts.length > 1 && !parts[1].isEmpty()) {
                strings.put(FIELD_COUNTRY, parts[1].toUpperCase(Locale.ROOT));
            }
        }

        strings.put(FIELD_DEVICE_TYPE, device.isTablet ? DEVICE_TYPE_TABLET : DEVICE_TYPE_PHONE);
        strings.put(FIELD_OS, "android");
        putIfNotNull(strings, FIELD_BUNDLE, device.packageName);
        putIfNotNull(strings, FIELD_MANUFACTURER, lowerOrNull(device.manufacturer));
        putIfNotNull(strings, FIELD_MODEL, lowerOrNull(device.model));

        numbers.put(FIELD_CONNECTION_TYPE, (double) device.connectionType);

        return new AudienceSignals(strings, numbers);
    }

    /** Test/advanced factory from explicit values. */
    @NonNull
    public static AudienceSignals of(@NonNull Map<String, String> strings,
                                     @NonNull Map<String, Double> numbers) {
        return new AudienceSignals(new HashMap<>(strings), new HashMap<>(numbers));
    }

    private static void putIfNotNull(Map<String, String> map, String key, @Nullable String value) {
        if (value != null) map.put(key, value);
    }

    @Nullable
    private static String lowerOrNull(@Nullable String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}
