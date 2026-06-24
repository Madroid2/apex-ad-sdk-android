package com.apexads.sdk.core.di;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class FeatureRegistryTest {

    @Test
    public void copyFrom_preserves_registered_features() {
        FeatureRegistry source = new FeatureRegistry();
        FeatureRegistry target = new FeatureRegistry();
        TestFeature feature = new TestFeatureImpl();

        source.register(TestFeature.class, feature);
        target.copyFrom(source);

        assertThat(target.getRequired(TestFeature.class)).isSameInstanceAs(feature);
    }

    @Test
    public void register_rejects_feature_for_wrong_contract() {
        FeatureRegistry registry = new FeatureRegistry();

        assertThrows(IllegalArgumentException.class,
                () -> registerWrongContract(registry));
    }

    @Test
    public void clear_removes_features() {
        FeatureRegistry registry = new FeatureRegistry();

        registry.register(TestFeature.class, new TestFeatureImpl());
        registry.clear();

        assertThat(registry.isRegistered(TestFeature.class)).isFalse();
    }

    interface TestFeature extends SdkFeature {}

    interface OtherFeature extends SdkFeature {}

    static final class TestFeatureImpl implements TestFeature {}

    static final class OtherFeatureImpl implements OtherFeature {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void registerWrongContract(FeatureRegistry registry) {
        registry.register((Class) TestFeature.class, new OtherFeatureImpl());
    }
}
