package com.apexads.sdk;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.apexads.sdk.core.audience.AudienceSignals;
import com.apexads.sdk.core.audience.Cohort;
import com.apexads.sdk.core.audience.CohortProvider;
import com.apexads.sdk.core.di.SdkFeature;
import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;
import com.apexads.sdk.core.network.AdNetworkClient;
import com.apexads.sdk.internal.ApexFeatureAccess;
import com.apexads.sdk.internal.ApexSdkRuntime;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class ApexAdsServicesTest {

    @After
    public void tearDown() {
        ApexSdkRuntime.reset();
    }

    @Test
    public void init_builds_typed_core_services() {
        Application application = ApplicationProvider.getApplicationContext();

        ApexAds.init(application, config());

        assertThat(ApexSdkRuntime.getContext()).isSameInstanceAs(application.getApplicationContext());
        assertThat(ApexSdkRuntime.getConfig().getAppToken()).isEqualTo("test-token");
        assertThat(ApexSdkRuntime.getNetworkClient()).isNotNull();
        assertThat(ApexSdkRuntime.getDeviceInfoProvider()).isNotNull();
        assertThat(ApexSdkRuntime.getConsentManager()).isNotNull();
        assertThat(ApexSdkRuntime.getCohortProvider()).isSameInstanceAs(CohortProvider.NONE);
    }

    @Test
    public void feature_registered_before_init_is_available_after_init() {
        TestFeature feature = new TestFeatureImpl();

        ApexFeatureAccess.registerFeature(TestFeature.class, feature);
        ApexAds.init(ApplicationProvider.getApplicationContext(), config());

        assertThat(ApexFeatureAccess.getFeature(TestFeature.class)).isSameInstanceAs(feature);
    }

    @Test
    public void reset_clears_services_and_pending_features() {
        ApexFeatureAccess.registerFeature(TestFeature.class, new TestFeatureImpl());
        ApexSdkRuntime.reset();

        assertThat(ApexFeatureAccess.getFeature(TestFeature.class)).isNull();
        assertThrows(IllegalStateException.class, ApexSdkRuntime::getConfig);
    }

    @Test
    public void reset_afterInit_clears_installed_features_and_requires_reinit() {
        ApexAds.init(ApplicationProvider.getApplicationContext(), config());
        ApexFeatureAccess.registerFeature(TestFeature.class, new TestFeatureImpl());

        ApexSdkRuntime.reset();

        assertThat(ApexFeatureAccess.getFeature(TestFeature.class)).isNull();
        assertThat(ApexSdkRuntime.isInitialized()).isFalse();
        assertThrows(IllegalStateException.class, ApexSdkRuntime::getNetworkClient);
    }

    @Test
    public void setAudienceCohortRules_updates_typed_provider_and_blank_restores_none() {
        ApexAds.init(ApplicationProvider.getApplicationContext(), config());

        ApexSdkRuntime.setAudienceCohortRules("{ \"cohorts\": [ { \"id\": \"de\", "
                + "\"match\": { \"field\": \"language\", \"op\": \"eq\", \"value\": \"de\" } } ] }");

        assertThat(ApexSdkRuntime.getCohortProvider()).isNotSameInstanceAs(CohortProvider.NONE);
        assertThat(ids(ApexSdkRuntime.getCohortProvider().resolve(signals("de")))).containsExactly("de");

        ApexSdkRuntime.setAudienceCohortRules(" ");

        assertThat(ApexSdkRuntime.getCohortProvider()).isSameInstanceAs(CohortProvider.NONE);
    }

    @Test
    public void setNetworkClientForTesting_replaces_only_network_client() {
        ApexAds.init(ApplicationProvider.getApplicationContext(), config());
        AdNetworkClient original = ApexSdkRuntime.getNetworkClient();
        FakeNetworkClient fake = new FakeNetworkClient();

        ApexSdkRuntime.setNetworkClientForTesting(fake);

        assertThat(ApexSdkRuntime.getNetworkClient()).isSameInstanceAs(fake);
        assertThat(ApexSdkRuntime.getDeviceInfoProvider()).isNotNull();
        assertThat(ApexSdkRuntime.getNetworkClient()).isNotSameInstanceAs(original);
    }

    @Test
    public void apexAds_publicApi_exposesOnlyInit() {
        Set<String> publicMethods = new HashSet<>();
        for (Method method : ApexAds.class.getMethods()) {
            if (method.getDeclaringClass().equals(ApexAds.class)) {
                publicMethods.add(method.getName());
            }
        }

        assertThat(publicMethods).containsExactly("init");
    }

    private static ApexAdsConfig config() {
        return new ApexAdsConfig.Builder("test-token")
                .debugLogging(false)
                .testMode(true)
                .build();
    }

    interface TestFeature extends SdkFeature {}

    static final class TestFeatureImpl implements TestFeature {}

    private static AudienceSignals signals(String language) {
        Map<String, String> strings = new HashMap<>();
        strings.put(AudienceSignals.FIELD_LANGUAGE, language);
        return AudienceSignals.of(strings, Collections.emptyMap());
    }

    private static List<String> ids(List<Cohort> cohorts) {
        List<String> ids = new ArrayList<>();
        for (Cohort cohort : cohorts) {
            ids.add(cohort.id());
        }
        return ids;
    }

    static final class FakeNetworkClient implements AdNetworkClient {
        @NonNull
        @Override
        public BidResponse requestBid(@NonNull BidRequest request) {
            return new BidResponse();
        }

        @Override
        public void fireTrackingUrl(@NonNull String url) {}
    }
}
