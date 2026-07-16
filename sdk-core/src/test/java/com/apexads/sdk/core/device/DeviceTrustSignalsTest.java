package com.apexads.sdk.core.device;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class DeviceTrustSignalsTest {

    @Test
    public void evaluate_knownEmulator_isHighRisk() {
        DeviceTrustSignals.Result result = DeviceTrustSignals.evaluate(
                "google/sdk_gphone64_arm64/emu64a:15/test-keys",
                "sdk_gphone64_arm64", "Google", "google", "emu64a",
                "sdk_gphone64_arm64", "ranchu");

        assertThat(result.risk).isEqualTo("HIGH");
        assertThat(result.emulatorSuspected).isTrue();
    }

    @Test
    public void evaluate_typicalProductionDevice_isLowRisk() {
        DeviceTrustSignals.Result result = DeviceTrustSignals.evaluate(
                "google/husky/husky:15/AP4A.250105.002/1234567:user/release-keys",
                "Pixel 8 Pro", "Google", "google", "husky", "husky", "husky");

        assertThat(result.risk).isEqualTo("LOW");
        assertThat(result.emulatorSuspected).isFalse();
    }
}
