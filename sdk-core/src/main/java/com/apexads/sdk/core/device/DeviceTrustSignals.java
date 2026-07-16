package com.apexads.sdk.core.device;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Locale;

/**
 * Low-cost emulator heuristics. These signals are explicitly advisory: a
 * modified client can spoof them, so the server must never treat them as an
 * attestation or as the sole reason to bill/reject an impression.
 */
public final class DeviceTrustSignals {

    public static final int VERSION = 1;

    public static final class Result {
        @NonNull public final String risk;
        public final boolean emulatorSuspected;

        Result(@NonNull String risk, boolean emulatorSuspected) {
            this.risk = risk;
            this.emulatorSuspected = emulatorSuspected;
        }
    }

    private DeviceTrustSignals() {}

    @NonNull
    public static Result evaluate() {
        return evaluate(Build.FINGERPRINT, Build.MODEL, Build.MANUFACTURER,
                Build.BRAND, Build.DEVICE, Build.PRODUCT, Build.HARDWARE);
    }

    @VisibleForTesting
    @NonNull
    static Result evaluate(String fingerprint, String model, String manufacturer,
                           String brand, String device, String product, String hardware) {
        String fp = normalized(fingerprint);
        String m = normalized(model);
        String maker = normalized(manufacturer);
        String b = normalized(brand);
        String d = normalized(device);
        String p = normalized(product);
        String hw = normalized(hardware);

        int score = 0;
        if (fp.startsWith("generic") || fp.startsWith("unknown") || fp.contains("test-keys")) score += 30;
        if (m.contains("google_sdk") || m.contains("emulator") || m.contains("android sdk")) score += 45;
        if (maker.contains("genymotion") || maker.contains("netease")) score += 55;
        if (b.startsWith("generic") && d.startsWith("generic")) score += 30;
        if (p.contains("sdk") || p.contains("gphone") || p.contains("vbox")) score += 35;
        if (hw.contains("goldfish") || hw.contains("ranchu") || hw.contains("vbox")) score += 50;

        String risk = score >= 75 ? "HIGH" : score >= 30 ? "MEDIUM" : "LOW";
        return new Result(risk, score >= 60);
    }

    private static String normalized(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US);
    }
}
