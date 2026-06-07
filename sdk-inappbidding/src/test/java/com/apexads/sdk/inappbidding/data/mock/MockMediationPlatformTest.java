package com.apexads.sdk.inappbidding.mock;

import static com.google.common.truth.Truth.assertThat;

import com.apexads.sdk.inappbidding.BidToken;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

public class MockMediationPlatformTest {

    @Test
    public void simulateLoad_withoutTokenFallsBack() {
        assertThat(new MockMediationPlatform().simulateLoad()).isEqualTo("WaterfallFallbackNetwork");
    }

    @Test
    public void simulateLoad_selectsTierByCpm() throws Exception {
        assertThat(winnerFor(5.0)).isEqualTo("ApexAds (Tier1)");
        assertThat(winnerFor(2.0)).isEqualTo("ApexAds (Tier2)");
        assertThat(winnerFor(1.99)).isEqualTo("WaterfallFallbackNetwork");
    }

    @Test
    public void simulateLoad_expiredTokenFallsBack() throws Exception {
        MockMediationPlatform platform = new MockMediationPlatform();
        BidToken token = token(7.0);
        Field createdAt = BidToken.class.getDeclaredField("createdAtMs");
        createdAt.setAccessible(true);
        createdAt.setLong(token, System.currentTimeMillis() - BidToken.TTL_MS - 1_000L);
        platform.setApexBidToken(token);

        assertThat(platform.simulateLoad()).isEqualTo("WaterfallFallbackNetwork");
    }

    @Test
    public void simulateImpression_reportsWinningNetworkAndCpm() throws Exception {
        MockMediationPlatform platform = new MockMediationPlatform();
        platform.setApexBidToken(token(2.25));
        AtomicReference<String> network = new AtomicReference<>();
        AtomicReference<Double> cpm = new AtomicReference<>();

        platform.simulateImpression((winningNetwork, price) -> {
            network.set(winningNetwork);
            cpm.set(price);
        });

        assertThat(network.get()).isEqualTo("ApexAds (Tier2)");
        assertThat(cpm.get()).isEqualTo(2.25);
    }

    private static String winnerFor(double cpm) throws Exception {
        MockMediationPlatform platform = new MockMediationPlatform();
        platform.setApexBidToken(token(cpm));
        return platform.simulateLoad();
    }

    private static BidToken token(double cpm) throws Exception {
        Constructor<BidToken> constructor = BidToken.class
                .getDeclaredConstructor(String.class, double.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance("placement", cpm, "token");
    }
}
