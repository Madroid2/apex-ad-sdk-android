package com.apexads.demo;

import static com.google.common.truth.Truth.assertThat;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.apexads.sdk.ApexAds;
import com.apexads.sdk.ApexAdsConfig;
import com.apexads.sdk.banner.BannerAd;
import com.apexads.sdk.banner.BannerAdListener;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.request.OpenRTBRequestBuilder;
import com.apexads.sdk.internal.ApexSdkRuntime;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sends real SDK-built auction requests to a developer-supplied Ad Server.
 *
 * <p>The test intentionally never calls {@code show()}. Consequently it never
 * renders an ad and never fires win, bill, viewable-impression, click, or
 * conversion trackers. Every request is test mode, LMT/DNT, and identity-free.</p>
 */
@RunWith(AndroidJUnit4.class)
public class ApexIntelligenceLoadInstrumentationTest {

    private static final String DEFAULT_ENDPOINT =
            "http://10.0.2.2:18080/openrtb/v1/auction";
    private static final int DEFAULT_LOAD_COUNT = 250;
    private static final int DEFAULT_BATCH_SIZE = 25;

    private int loadCount;
    private int batchSize;

    @Before
    public void setUp() {
        ApexSdkRuntime.reset();
        Bundle arguments = InstrumentationRegistry.getArguments();
        loadCount = boundedInt(arguments.getString("apexLoadCount"), DEFAULT_LOAD_COUNT, 1, 10_000);
        batchSize = boundedInt(arguments.getString("apexBatchSize"), DEFAULT_BATCH_SIZE, 1, 100);
        String endpoint = arguments.getString("apexAdServerUrl", DEFAULT_ENDPOINT);

        Application application = ApplicationProvider.getApplicationContext();
        ApexAds.init(application, new ApexAdsConfig.Builder("apex-intelligence-synthetic")
                .adServerUrl(endpoint)
                .trackingUrl("http://10.0.2.2:18080")
                .requestTimeoutMs(2_000)
                .cacheTtlSeconds(1)
                .coppa(1)
                .testMode(true)
                .debugFakeFill(false)
                .debugLogging(false)
                .build());
    }

    @After
    public void tearDown() {
        ApexSdkRuntime.reset();
    }

    @Test
    public void sendsSyntheticAuctionLoadsWithoutRenderingOrTracking() throws Exception {
        BidRequest identityProbe = new OpenRTBRequestBuilder(
                ApexSdkRuntime.getDeviceInfoProvider(),
                ApexSdkRuntime.getConsentManager())
                .placementId("identity-safety-probe")
                .build();
        assertThat(identityProbe.device.ifa).isNull();
        assertThat(identityProbe.user.id).isNull();
        assertThat(identityProbe.device.lmt).isEqualTo(1);
        assertThat(identityProbe.device.dnt).isEqualTo(1);

        AtomicInteger loaded = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        for (int offset = 0; offset < loadCount; offset += batchSize) {
            int batchCount = Math.min(batchSize, loadCount - offset);
            CountDownLatch completed = new CountDownLatch(batchCount);
            List<BannerAd> batch = new ArrayList<>(batchCount);

            for (int index = 0; index < batchCount; index++) {
                int sequence = offset + index;
                BannerAd ad = new BannerAd.Builder("intelligence-synthetic-" + sequence)
                        .adSize(AdSize.BANNER_320x50)
                        .bidFloor(0.05)
                        .listener(new BannerAdListener() {
                            @Override
                            public void onAdLoaded() {
                                loaded.incrementAndGet();
                                completed.countDown();
                            }

                            @Override
                            public void onAdFailed(@NonNull AdError error) {
                                failed.incrementAndGet();
                                completed.countDown();
                            }
                        })
                        .build();
                batch.add(ad);
                ad.load();
            }

            assertThat(completed.await(30, TimeUnit.SECONDS)).isTrue();
            for (BannerAd ad : batch) {
                ad.destroy();
            }
        }

        int completed = loaded.get() + failed.get();
        System.out.println("APEX_SYNTHETIC_LOAD_SUMMARY requested=" + loadCount
                + " batch_size=" + batchSize
                + " completed=" + completed
                + " loaded=" + loaded.get()
                + " failed=" + failed.get());
        assertThat(completed).isEqualTo(loadCount);
        assertThat(loaded.get()).isGreaterThan(0);
    }

    private static int boundedInt(String value, int fallback, int minimum, int maximum) {
        if (value == null) return fallback;
        try {
            return Math.max(minimum, Math.min(maximum, Integer.parseInt(value)));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
