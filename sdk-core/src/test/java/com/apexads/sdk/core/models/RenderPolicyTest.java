package com.apexads.sdk.core.models;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class RenderPolicyTest {

    @Test
    public void parsesServerPolicyFromResponseExt() {
        RenderPolicy policy = RenderPolicy.fromResponseExt(
                "{\"apex\":{\"render\":{\"refresh_seconds\":45,\"width_dp\":320,"
                        + "\"height_dp\":50,\"close_after_seconds\":8,\"muted\":true,"
                        + "\"max_per_session\":6}}}");

        assertThat(policy.refreshSeconds).isEqualTo(45);
        assertThat(policy.widthDp).isEqualTo(320);
        assertThat(policy.heightDp).isEqualTo(50);
        assertThat(policy.closeAfterSeconds).isEqualTo(8);
        assertThat(policy.muted).isTrue();
        assertThat(policy.maxPerSession).isEqualTo(6);
        assertThat(policy.refreshes()).isTrue();
    }

    @Test
    public void absentPolicyFallsBackToDefault() {
        assertThat(RenderPolicy.fromResponseExt(null)).isSameInstanceAs(RenderPolicy.DEFAULT);
        assertThat(RenderPolicy.fromResponseExt("")).isSameInstanceAs(RenderPolicy.DEFAULT);
        assertThat(RenderPolicy.fromResponseExt("{}")).isSameInstanceAs(RenderPolicy.DEFAULT);
        assertThat(RenderPolicy.fromResponseExt("{\"apex\":{}}")).isSameInstanceAs(RenderPolicy.DEFAULT);
        assertThat(RenderPolicy.DEFAULT.refreshes()).isFalse();
    }

    @Test
    public void malformedExtNeverThrows() {
        assertThat(RenderPolicy.fromResponseExt("not json")).isSameInstanceAs(RenderPolicy.DEFAULT);
        assertThat(RenderPolicy.fromResponseExt("{\"apex\":\"scalar\"}"))
                .isSameInstanceAs(RenderPolicy.DEFAULT);
    }

    // The SDK re-clamps because this drives a loop on the user's battery: a
    // hostile or misconfigured response must not be able to spin the slot.
    @Test
    public void refreshBelowFloorIsTreatedAsOffNotRaised() {
        RenderPolicy policy = RenderPolicy.fromResponseExt(
                "{\"apex\":{\"render\":{\"refresh_seconds\":1}}}");

        assertThat(policy.refreshSeconds).isEqualTo(0);
        assertThat(policy.refreshes()).isFalse();
    }

    @Test
    public void refreshAboveCeilingIsTreatedAsOff() {
        RenderPolicy policy = RenderPolicy.fromResponseExt(
                "{\"apex\":{\"render\":{\"refresh_seconds\":99999}}}");

        assertThat(policy.refreshes()).isFalse();
    }

    @Test
    public void absurdDimensionsAndCapsAreClamped() {
        RenderPolicy policy = RenderPolicy.fromResponseExt(
                "{\"apex\":{\"render\":{\"width_dp\":999999,\"height_dp\":-5,"
                        + "\"close_after_seconds\":99999,\"max_per_session\":-3}}}");

        assertThat(policy.widthDp).isAtMost(4000);
        assertThat(policy.heightDp).isEqualTo(0);
        assertThat(policy.closeAfterSeconds).isAtMost(120);
        assertThat(policy.maxPerSession).isEqualTo(0);
    }
}
