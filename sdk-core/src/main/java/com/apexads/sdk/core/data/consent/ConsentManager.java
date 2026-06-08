package com.apexads.sdk.core.consent;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.utils.AdLog;

public final class ConsentManager {

    public static final String KEY_TCF_STRING = "IABTCF_TCString";
    public static final String KEY_GDPR_APPLIES = "IABTCF_gdprApplies";
    public static final String KEY_PURPOSE_CONSENTS = "IABTCF_PurposeConsents";
    public static final String KEY_VENDOR_CONSENTS = "IABTCF_VendorConsents";
    public static final String KEY_CMP_SDK_ID = "IABTCF_CmpSdkID";
    public static final String KEY_CMP_SDK_VERSION = "IABTCF_CmpSdkVersion";

    public static final String KEY_US_PRIVACY = "IABUSPrivacy_String";

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

    public boolean hasStorageConsent() {
        String purposeConsents = prefs.getString(KEY_PURPOSE_CONSENTS, null);
        return purposeConsents != null && !purposeConsents.isEmpty() && purposeConsents.charAt(0) == '1';
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
