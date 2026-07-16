package com.apexads.sdk.integrity;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class TrustProtocolTest {

    @Test
    public void envelope_isCanonicalAcrossServices() {
        assertThat(TrustProtocol.envelope(
                "post", "/openrtb/v1/auction?debug=1", 1_800_000_000L,
                "nonce-value", 42L, "body-hash"))
                .isEqualTo("APEX1\nPOST\n/openrtb/v1/auction?debug=1\n"
                        + "1800000000\nnonce-value\n42\nbody-hash");
    }

    @Test
    public void leaseRequestHash_matchesServerProtocol() {
        assertThat(TrustProtocol.leaseRequestHash(
                "challenge-id", "challenge-value", "com.example.app",
                "key-id", "public-hash"))
                .isEqualTo("8u9CVcGvDKzc_m7HkOUKYXHqVW7zWoKX5jJtDVrG6Ko");
    }
}
