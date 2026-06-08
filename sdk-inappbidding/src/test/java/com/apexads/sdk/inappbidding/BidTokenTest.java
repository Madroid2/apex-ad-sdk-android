package com.apexads.sdk.inappbidding;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.lang.reflect.Field;

public class BidTokenTest {

    @Test
    public void isExpired_falseForFreshToken() {
        BidToken token = new BidToken("placement", 2.5, "opaque-token");

        assertThat(token.isExpired()).isFalse();
    }

    @Test
    public void isExpired_truePastTtl() throws Exception {
        BidToken token = new BidToken("placement", 2.5, "opaque-token");
        Field createdAt = BidToken.class.getDeclaredField("createdAtMs");
        createdAt.setAccessible(true);
        createdAt.setLong(token, System.currentTimeMillis() - BidToken.TTL_MS - 1_000L);

        assertThat(token.isExpired()).isTrue();
    }

    @Test
    public void toString_redactsOpaqueToken() {
        BidToken token = new BidToken("placement", 2.5, "opaque-token");

        assertThat(token.toString()).contains("placement");
        assertThat(token.toString()).contains("2.5");
        assertThat(token.toString()).doesNotContain("opaque-token");
    }
}
