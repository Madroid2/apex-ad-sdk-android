package com.apexads.sdk.core.consent;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.utils.AdLog;

/**
 * Reads IAB TCF 2.0 and US Privacy consent signals from SharedPreferences.
 *
 * Publishers surface a CMP (Consent Management Platform) which writes the
 * standardised IAB keys. This class reads them and wires them into OpenRTB.
 * Ref: https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework
 */
public final class ConsentManager {

    private static final String PREFS_NAME = "IABConsent_Settings";

    // IAB TCF 2.0 standard SharedPreferences keys
    private static final String KEY_TCF_STRING      = "IABTCF_TCString";
    private static final String KEY_GDPR_APPLIES    = "IABTCF_gdprApplies";
    private static final String KEY_PURPOSE_CONSENTS = "IABTCF_PurposeConsents";

    // IAB US Privacy (CCPA)
    private static final String KEY_US_PRIVACY      = "IABUSPrivacy_String";

    private final SharedPreferences prefs;

    public ConsentManager(@NonNull Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** TCF 2.0 consent string set by the publisher's CMP, or null if absent. */
    @Nullable
    public String getTcfConsentString() {
        return prefs.getString(KEY_TCF_STRING, null);
    }

    /** Whether GDPR applies for this user (read from CMP-written key). */
    public boolean isGdprApplicable() {
        return prefs.getInt(KEY_GDPR_APPLIES, 0) == 1;
    }

    /** IAB US Privacy string (CCPA), e.g. "1YNN". */
    @Nullable
    public String getUsPrivacyString() {
        return prefs.getString(KEY_US_PRIVACY, null);
    }

    /** Simple consent check for TCF Purpose 1 (store/access information on a device). */
    public boolean hasStorageConsent() {
        String purposeConsents = prefs.getString(KEY_PURPOSE_CONSENTS, null);
        return purposeConsents != null && !purposeConsents.isEmpty() && purposeConsents.charAt(0) == '1';
    }

    /**
     * Programmatically sets GDPR consent (for non-CMP flows or testing).
     * IAB-compliant CMPs will overwrite these keys automatically.
     */
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
