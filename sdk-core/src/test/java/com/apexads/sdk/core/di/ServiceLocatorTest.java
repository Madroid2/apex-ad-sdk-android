package com.apexads.sdk.core.di;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.apexads.sdk.internal.ApexSdkRuntime;

import org.junit.After;
import org.junit.Test;

public class ServiceLocatorTest {

    @After
    public void tearDown() {
        ApexSdkRuntime.reset();
    }

    @Test
    public void register_and_get_returns_feature() {
        TestFeature feature = new TestFeatureImpl();

        ServiceLocator.register(TestFeature.class, feature);

        assertThat(ServiceLocator.get(TestFeature.class)).isSameInstanceAs(feature);
    }

    @Test
    public void getOptional_returns_null_before_registration() {
        assertThat(ServiceLocator.getOptional(TestFeature.class)).isNull();
    }

    @Test
    public void get_throws_for_unregistered_feature() {
        assertThrows(IllegalStateException.class, () -> ServiceLocator.get(TestFeature.class));
    }

    @Test
    public void register_same_implementation_is_idempotent() {
        TestFeature first = new TestFeatureImpl();
        TestFeature second = new TestFeatureImpl();

        ServiceLocator.register(TestFeature.class, first);
        ServiceLocator.register(TestFeature.class, second);

        assertThat(ServiceLocator.get(TestFeature.class)).isSameInstanceAs(first);
    }

    @Test
    public void register_different_implementation_throws_and_keeps_original() {
        TestFeature first = new TestFeatureImpl();

        ServiceLocator.register(TestFeature.class, first);

        assertThrows(IllegalStateException.class,
                () -> ServiceLocator.register(TestFeature.class, new ReplacementFeatureImpl()));
        assertThat(ServiceLocator.get(TestFeature.class)).isSameInstanceAs(first);
    }

    interface TestFeature extends SdkFeature {}

    static final class TestFeatureImpl implements TestFeature {}

    static final class ReplacementFeatureImpl implements TestFeature {}
}
