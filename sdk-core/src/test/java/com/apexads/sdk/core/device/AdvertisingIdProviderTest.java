package com.apexads.sdk.core.device;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AdvertisingIdProviderTest {

    @Test
    public void get_beforeAnyRefresh_returnsEmptySnapshot() {
        AdvertisingIdProvider provider =
                new AdvertisingIdProvider(ApplicationProvider.getApplicationContext());

        assertThat(provider.get().id).isNull();
        assertThat(provider.get().limitAdTracking).isFalse();
    }

    @Test
    public void normalize_zeroedId_treatedAsOptOut() {
        AdvertisingIdProvider.Info info = AdvertisingIdProvider.normalize(
                new AdvertisingIdProvider.Info("00000000-0000-0000-0000-000000000000", false));

        assertThat(info.id).isNull();
        assertThat(info.limitAdTracking).isTrue();
    }

    @Test
    public void normalize_blankOrNull_yieldsNullId() {
        assertThat(AdvertisingIdProvider.normalize(
                new AdvertisingIdProvider.Info("  ", false)).id).isNull();
        assertThat(AdvertisingIdProvider.normalize(
                new AdvertisingIdProvider.Info(null, true)).limitAdTracking).isTrue();
        assertThat(AdvertisingIdProvider.normalize(null).id).isNull();
    }

    @Test
    public void normalize_validId_preserved() {
        AdvertisingIdProvider.Info info = AdvertisingIdProvider.normalize(
                new AdvertisingIdProvider.Info("38400000-8cf0-11bd-b23e-10b96e40000d", false));

        assertThat(info.id).isEqualTo("38400000-8cf0-11bd-b23e-10b96e40000d");
        assertThat(info.limitAdTracking).isFalse();
    }

    @Test
    public void refreshBlocking_usesFireOsSettingsFallback() {
        Context context = ApplicationProvider.getApplicationContext();
        Settings.Secure.putString(context.getContentResolver(),
                "advertising_id", "fire-os-id");
        Settings.Secure.putInt(context.getContentResolver(),
                "limit_ad_tracking", 0);

        AdvertisingIdProvider provider = new AdvertisingIdProvider(context);
        provider.setBindTimeoutForTesting(50L);
        AdvertisingIdProvider.Info info = provider.refreshBlocking();

        assertThat(info.id).isEqualTo("fire-os-id");
        assertThat(info.limitAdTracking).isFalse();
        assertThat(provider.get().id).isEqualTo("fire-os-id");
    }

    @Test
    public void refreshBlocking_fireOsOptOut_reportsLat() {
        Context context = ApplicationProvider.getApplicationContext();
        Settings.Secure.putString(context.getContentResolver(),
                "advertising_id", "fire-os-id");
        Settings.Secure.putInt(context.getContentResolver(),
                "limit_ad_tracking", 1);

        AdvertisingIdProvider provider = new AdvertisingIdProvider(context);
        provider.setBindTimeoutForTesting(50L);
        AdvertisingIdProvider.Info info = provider.refreshBlocking();

        assertThat(info.limitAdTracking).isTrue();
    }

    @Test
    public void refreshBlocking_noProviderAvailable_returnsEmpty() {
        AdvertisingIdProvider provider = new AdvertisingIdProvider(
                ApplicationProvider.getApplicationContext());
        provider.setBindTimeoutForTesting(50L);
        AdvertisingIdProvider.Info info = provider.refreshBlocking();

        assertThat(info.id).isNull();
        assertThat(info.limitAdTracking).isFalse();
    }
}
