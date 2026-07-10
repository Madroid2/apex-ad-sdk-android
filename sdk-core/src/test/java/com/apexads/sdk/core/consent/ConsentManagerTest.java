package com.apexads.sdk.core.consent;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ConsentManagerTest {

    private Context context;
    private SharedPreferences prefs;
    private ConsentManager consentManager;

    @Before
    @SuppressWarnings("deprecation")
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().clear().commit();
        consentManager = new ConsentManager(context);
    }

    @Test
    public void canShareDeviceIdentifiers_gdprNotApplicable_allowed() {
        prefs.edit().putInt(ConsentManager.KEY_GDPR_APPLIES, 0).commit();

        assertThat(consentManager.canShareDeviceIdentifiers()).isTrue();
    }

    @Test
    public void canShareDeviceIdentifiers_gdprWithoutPurpose1_denied() {
        prefs.edit()
                .putInt(ConsentManager.KEY_GDPR_APPLIES, 1)
                .putString(ConsentManager.KEY_PURPOSE_CONSENTS, "0111")
                .commit();

        assertThat(consentManager.canShareDeviceIdentifiers()).isFalse();
    }

    @Test
    public void canShareDeviceIdentifiers_gdprWithPurpose1_allowed() {
        prefs.edit()
                .putInt(ConsentManager.KEY_GDPR_APPLIES, 1)
                .putString(ConsentManager.KEY_PURPOSE_CONSENTS, "1000")
                .commit();

        assertThat(consentManager.canShareDeviceIdentifiers()).isTrue();
    }

    @Test
    public void canShareDeviceIdentifiers_gdprWithNoPurposeString_denied() {
        prefs.edit().putInt(ConsentManager.KEY_GDPR_APPLIES, 1).commit();

        assertThat(consentManager.canShareDeviceIdentifiers()).isFalse();
    }

    @Test
    public void hasPersonalizationConsent_ccpaOptOut_denied() {
        prefs.edit()
                .putInt(ConsentManager.KEY_GDPR_APPLIES, 0)
                .putString(ConsentManager.KEY_US_PRIVACY, "1YYN")
                .commit();

        assertThat(consentManager.hasPersonalizationConsent()).isFalse();
    }
}
