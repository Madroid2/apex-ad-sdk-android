package com.apexads.sdk.core.consent;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.utils.AdLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class ConsentManager {

    // IAB TCF v2.x in-app keys, as defined by the "IAB Tech Lab - CMP API v2" spec
    // (NSUserDefaults / SharedPreferences storage section). These are NOT the legacy
    // TCF v1.1 mobile keys, which used the "IABConsent_" prefix and no per-purpose
    // bitfield — do not cross-reference the v1.1 "consent string formats" doc here.
    // IABTCF_PurposeConsents is a binary string, zero-indexed: char at position n is
    // the consent status for purpose ID n+1 (so index 3 == Purpose 4).
    public static final String KEY_TCF_STRING = "IABTCF_TCString";
    public static final String KEY_GDPR_APPLIES = "IABTCF_gdprApplies";
    public static final String KEY_PURPOSE_CONSENTS = "IABTCF_PurposeConsents";
    public static final String KEY_VENDOR_CONSENTS = "IABTCF_VendorConsents";
    public static final String KEY_CMP_SDK_ID = "IABTCF_CmpSdkID";
    public static final String KEY_CMP_SDK_VERSION = "IABTCF_CmpSdkVersion";

    public static final String KEY_US_PRIVACY = "IABUSPrivacy_String";
    public static final String KEY_GPP_STRING = "IABGPP_GppString";
    public static final String KEY_GPP_STRING_HEADER = "IABGPP_HDR_GppString";
    public static final String KEY_GPP_SECTION_IDS = "IABGPP_GppSID";

    /** IAB TCF purpose numbers (1-based) we gate on. */
    private static final int PURPOSE_STORAGE = 1;          // Store and/or access information on a device
    private static final int PURPOSE_PERSONALISED_ADS = 4; // Select personalised ads

    private final SharedPreferences prefs;

    public ConsentManager(@NonNull Context context) {

        prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Nullable
    public String getTcfConsentString() {
        return prefs.getString(KEY_TCF_STRING, null);
    }

    public boolean isGdprApplicable() {
        return prefs.getInt(KEY_GDPR_APPLIES, 0) == 1;
    }

    @Nullable
    public String getUsPrivacyString() {
        return prefs.getString(KEY_US_PRIVACY, null);
    }

    /** Returns the encoded IAB Global Privacy Platform string, when supplied by the CMP. */
    @Nullable
    public String getGppString() {
        Object value = prefs.getAll().get(KEY_GPP_STRING);
        if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
            value = prefs.getAll().get(KEY_GPP_STRING_HEADER);
        }
        return value instanceof String && !((String) value).trim().isEmpty()
                ? ((String) value).trim()
                : null;
    }

    /**
     * Applicable GPP section IDs. CMPs store these as an underscore/comma-delimited
     * SharedPreferences string; malformed tokens are ignored rather than fabricated.
     */
    @NonNull
    public List<Integer> getGppSectionIds() {
        Object raw = prefs.getAll().get(KEY_GPP_SECTION_IDS);
        if (raw == null) return Collections.emptyList();
        if (raw instanceof Number) {
            int id = ((Number) raw).intValue();
            return id > 0 ? Collections.singletonList(id) : Collections.emptyList();
        }
        if (raw instanceof Set<?>) {
            List<Integer> ids = new ArrayList<>();
            for (Object value : (Set<?>) raw) addGppSectionId(ids, String.valueOf(value));
            return Collections.unmodifiableList(ids);
        }
        String encoded = String.valueOf(raw).trim();
        if (encoded.isEmpty()) return Collections.emptyList();
        List<Integer> ids = new ArrayList<>();
        for (String token : encoded.split("[,_\\s]+")) {
            addGppSectionId(ids, token);
        }
        return Collections.unmodifiableList(ids);
    }

    private static void addGppSectionId(List<Integer> ids, String token) {
        try {
            int id = Integer.parseInt(token.trim());
            if (id > 0 && !ids.contains(id)) ids.add(id);
        } catch (NumberFormatException ignored) {
        }
    }

    public boolean hasStorageConsent() {
        return hasPurposeConsent(PURPOSE_STORAGE);
    }

    /**
     * Whether the user consented to personalised advertising (IAB TCF Purpose 4 —
     * "Select personalised ads"). Required before attaching first-party audience
     * cohorts to the bid request.
     *
     * <p>Reads the CMP-decoded {@code IABTCF_PurposeConsents} bitfield straight from
     * shared preferences — we never decode the raw TC string on-device, that work is
     * already done by the CMP (and any deeper validation belongs server-side).</p>
     */
    public boolean hasPersonalizationConsent() {
        // A US Privacy opt-out of sale/sharing forbids personalised targeting outright.
        if (isCcpaOptOut()) {
            return false;
        }
        // When GDPR does not apply there is nothing further to gate on.
        return !isGdprApplicable() || hasPurposeConsent(PURPOSE_PERSONALISED_ADS);
    }

    /**
     * Whether device identifiers (GAID) may leave the device in the bid request.
     * Under GDPR this requires IAB TCF Purpose 1 ("Store and/or access information
     * on a device"); outside GDPR jurisdictions no purpose gate applies. LAT and
     * COPPA suppression are enforced separately by the request builder.
     */
    public boolean canShareDeviceIdentifiers() {
        return !isGdprApplicable() || hasPurposeConsent(PURPOSE_STORAGE);
    }

    /**
     * @return true if the IAB US Privacy string signals an opt-out of sale/sharing.
     *         Format: version char + 3 flag chars; index 2 ('Y'/'N') is the opt-out flag.
     */
    private boolean isCcpaOptOut() {
        String usPrivacy = getUsPrivacyString();
        return usPrivacy != null && usPrivacy.length() >= 3
                && Character.toUpperCase(usPrivacy.charAt(2)) == 'Y';
    }

    /**
     * @param purposeNumber 1-based IAB TCF purpose number.
     * @return true if the purpose bit is set in {@code IABTCF_PurposeConsents}.
     */
    private boolean hasPurposeConsent(int purposeNumber) {
        String purposeConsents = prefs.getString(KEY_PURPOSE_CONSENTS, null);
        int index = purposeNumber - 1;
        return purposeConsents != null && purposeConsents.length() > index
                && purposeConsents.charAt(index) == '1';
    }

    public void setGdprConsent(boolean applies, @Nullable String consentString) {
        SharedPreferences.Editor editor = prefs.edit()
                .putInt(KEY_GDPR_APPLIES, applies ? 1 : 0);
        if (consentString != null) {
            editor.putString(KEY_TCF_STRING, consentString);
        } else {
            editor.remove(KEY_TCF_STRING);
        }
        editor.apply();
        AdLog.d("ConsentManager: GDPR applies=%b", applies);
    }

    public void setUsPrivacyString(@Nullable String usPrivacy) {
        SharedPreferences.Editor editor = prefs.edit();
        if (usPrivacy != null) editor.putString(KEY_US_PRIVACY, usPrivacy);
        else editor.remove(KEY_US_PRIVACY);
        editor.apply();
    }
}
