package com.apexads.demo;

import static com.google.common.truth.Truth.assertThat;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.apexads.sdk.ApexAds;
import com.apexads.sdk.ApexAdsConfig;
import com.apexads.sdk.appopen.AppOpenAd;
import com.apexads.sdk.banner.BannerAd;
import com.apexads.sdk.banner.BannerAdListener;
import com.apexads.sdk.core.di.ServiceLocator;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.core.models.openrtb.BidResponse;
import com.apexads.sdk.core.network.AdNetworkClient;
import com.apexads.sdk.interstitial.InterstitialAd;
import com.apexads.sdk.interstitial.InterstitialAdListener;
import com.apexads.sdk.nativeads.NativeAd;
import com.apexads.sdk.nativeads.NativeAdListener;
import com.apexads.sdk.video.VideoAd;
import com.apexads.sdk.video.VideoAdListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class ApexAdResponseInstrumentationTest {

    private Application application;

    @Before
    public void setUp() {
        application = ApplicationProvider.getApplicationContext();
        AppOpenAd.destroy();
        ApexAds.reset();
        ApexAds.init(application, new ApexAdsConfig.Builder("instrumentation-token")
                .testMode(true)
                .debugLogging(false)
                .cacheTtlSeconds(60)
                .build());
    }

    @After
    public void tearDown() {
        AppOpenAd.destroy();
        ApexAds.reset();
    }

    @Test
    public void banner_handlesValidAndInvalidResponses() throws Exception {
        installFake(new FakeClient(FakeMode.VALID_BANNER));
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch failed = new CountDownLatch(1);
        BannerAd banner = new BannerAd.Builder("banner-placement")
                .adSize(AdSize.BANNER_320x50)
                .listener(new BannerAdListener() {
                    @Override public void onAdLoaded() { loaded.countDown(); }
                    @Override public void onAdFailed(@NonNull AdError error) { failed.countDown(); }
                })
                .build();

        banner.load();

        assertThat(loaded.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(failed.getCount()).isEqualTo(1);
        banner.destroy();

        installFake(new FakeClient(FakeMode.NO_FILL));
        CountDownLatch invalidFailed = new CountDownLatch(1);
        AtomicReference<AdError> error = new AtomicReference<>();
        new BannerAd.Builder("banner-placement")
                .listener(new BannerAdListener() {
                    @Override public void onAdLoaded() {}
                    @Override public void onAdFailed(@NonNull AdError adError) {
                        error.set(adError);
                        invalidFailed.countDown();
                    }
                })
                .build()
                .load();

        assertThat(invalidFailed.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).isInstanceOf(AdError.NoFill.class);
    }

    @Test
    public void interstitial_handlesValidAndInvalidResponses() throws Exception {
        installFake(new FakeClient(FakeMode.VALID_INTERSTITIAL));
        CountDownLatch loaded = new CountDownLatch(1);
        InterstitialAd interstitial = new InterstitialAd.Builder("interstitial-placement")
                .listener(new InterstitialAdListener() {
                    @Override public void onInterstitialLoaded() { loaded.countDown(); }
                    @Override public void onInterstitialFailed(@NonNull AdError error) {}
                })
                .build();

        interstitial.load();

        assertThat(loaded.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(interstitial.isReady()).isTrue();
        interstitial.destroy();

        installFake(new FakeClient(FakeMode.NO_FILL));
        CountDownLatch failed = new CountDownLatch(1);
        AtomicReference<AdError> error = new AtomicReference<>();
        new InterstitialAd.Builder("interstitial-placement")
                .listener(new InterstitialAdListener() {
                    @Override public void onInterstitialLoaded() {}
                    @Override public void onInterstitialFailed(@NonNull AdError adError) {
                        error.set(adError);
                        failed.countDown();
                    }
                })
                .build()
                .load();

        assertThat(failed.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).isInstanceOf(AdError.NoFill.class);
    }

    @Test
    public void nativeAd_handlesValidAndInvalidResponses() throws Exception {
        installFake(new FakeClient(FakeMode.VALID_NATIVE));
        CountDownLatch loaded = new CountDownLatch(1);
        AtomicReference<NativeAd> loadedAd = new AtomicReference<>();
        NativeAd nativeAd = new NativeAd.Builder("native-placement")
                .listener(new NativeAdListener() {
                    @Override public void onNativeAdLoaded(@NonNull NativeAd ad) {
                        loadedAd.set(ad);
                        loaded.countDown();
                    }
                    @Override public void onNativeAdFailed(@NonNull AdError error) {}
                })
                .build();

        nativeAd.load();

        assertThat(loaded.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(loadedAd.get().isReady()).isTrue();
        assertThat(loadedAd.get().getTitle()).isEqualTo("Instrumentation Native");
        nativeAd.destroy();

        installFake(new FakeClient(FakeMode.INVALID_NATIVE));
        CountDownLatch failed = new CountDownLatch(1);
        AtomicReference<AdError> error = new AtomicReference<>();
        new NativeAd.Builder("native-placement")
                .listener(new NativeAdListener() {
                    @Override public void onNativeAdLoaded(@NonNull NativeAd ad) {}
                    @Override public void onNativeAdFailed(@NonNull AdError adError) {
                        error.set(adError);
                        failed.countDown();
                    }
                })
                .build()
                .load();

        assertThat(failed.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).isInstanceOf(AdError.InvalidMarkup.class);
    }

    @Test
    public void video_handlesValidAndInvalidResponses() throws Exception {
        installFake(new FakeClient(FakeMode.VALID_VIDEO));
        CountDownLatch loaded = new CountDownLatch(1);
        VideoAd video = new VideoAd.Builder("video-placement")
                .listener(new VideoAdListener() {
                    @Override public void onVideoAdLoaded() { loaded.countDown(); }
                    @Override public void onVideoAdFailed(@NonNull AdError error) {}
                })
                .build();

        video.load();

        assertThat(loaded.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(video.isReady()).isTrue();
        video.destroy();

        installFake(new FakeClient(FakeMode.INVALID_VIDEO));
        CountDownLatch failed = new CountDownLatch(1);
        AtomicReference<AdError> error = new AtomicReference<>();
        new VideoAd.Builder("video-placement")
                .listener(new VideoAdListener() {
                    @Override public void onVideoAdLoaded() {}
                    @Override public void onVideoAdFailed(@NonNull AdError adError) {
                        error.set(adError);
                        failed.countDown();
                    }
                })
                .build()
                .load();

        assertThat(failed.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).isInstanceOf(AdError.InvalidMarkup.class);
    }

    @Test
    public void appOpen_handlesValidAndInvalidResponses() throws Exception {
        installFake(new FakeClient(FakeMode.VALID_INTERSTITIAL));
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch failed = new CountDownLatch(1);

        AppOpenAd.initialize(application, "appopen-placement", new AppOpenAd.Listener() {
            @Override public void onAppOpenAdLoaded() { loaded.countDown(); }
            @Override public void onAppOpenAdFailedToLoad(@NonNull AdError error) { failed.countDown(); }
        });

        assertThat(loaded.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(failed.getCount()).isEqualTo(1);
        assertThat(AppOpenAd.isAdReady()).isTrue();
        AppOpenAd.destroy();

        installFake(new FakeClient(FakeMode.NO_FILL));
        CountDownLatch invalidFailed = new CountDownLatch(1);
        AtomicReference<AdError> error = new AtomicReference<>();
        AppOpenAd.initialize(application, "appopen-placement", new AppOpenAd.Listener() {
            @Override public void onAppOpenAdLoaded() {}
            @Override public void onAppOpenAdFailedToLoad(@NonNull AdError adError) {
                error.set(adError);
                invalidFailed.countDown();
            }
        });

        assertThat(invalidFailed.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(error.get()).isInstanceOf(AdError.NoFill.class);
    }

    private static void installFake(AdNetworkClient client) {
        ServiceLocator.register(AdNetworkClient.class, client);
    }

    private static final class FakeClient implements AdNetworkClient {
        private final FakeMode mode;

        FakeClient(FakeMode mode) {
            this.mode = mode;
        }

        @NonNull
        @Override
        public BidResponse requestBid(@NonNull BidRequest request) {
            if (mode == FakeMode.NO_FILL) {
                return noFill(request.id);
            }
            BidRequest.Impression imp = request.imp.get(0);
            switch (mode) {
                case VALID_BANNER:
                    return response(request.id, bid(imp.id, "<html><body>banner</body></html>", 320, 50));
                case VALID_INTERSTITIAL:
                    return response(request.id, bid(imp.id, "<html><body>interstitial</body></html>", 320, 480));
                case VALID_NATIVE:
                    return response(request.id, bid(imp.id, NATIVE_JSON, 0, 0));
                case INVALID_NATIVE:
                    return response(request.id, bid(imp.id, "{\"native\":{\"assets\":[]}}", 0, 0));
                case VALID_VIDEO:
                    return response(request.id, bid(imp.id, VAST_XML, 640, 360));
                case INVALID_VIDEO:
                    return response(request.id, bid(imp.id, "<VAST version=\"4.0\"><Ad></Ad></VAST>", 640, 360));
                default:
                    return noFill(request.id);
            }
        }

        @Override
        public void fireTrackingUrl(@NonNull String url) {
        }
    }

    private enum FakeMode {
        VALID_BANNER,
        VALID_INTERSTITIAL,
        VALID_NATIVE,
        INVALID_NATIVE,
        VALID_VIDEO,
        INVALID_VIDEO,
        NO_FILL
    }

    private static BidResponse response(String requestId, BidResponse.Bid bid) {
        BidResponse.SeatBid seatBid = new BidResponse.SeatBid();
        seatBid.bid = Collections.singletonList(bid);
        BidResponse response = new BidResponse();
        response.id = requestId;
        response.cur = "USD";
        response.seatbid = Collections.singletonList(seatBid);
        return response;
    }

    private static BidResponse noFill(String requestId) {
        BidResponse response = new BidResponse();
        response.id = requestId;
        response.nbr = 2;
        response.seatbid = Collections.emptyList();
        return response;
    }

    private static BidResponse.Bid bid(String impId, String markup, int width, int height) {
        BidResponse.Bid bid = new BidResponse.Bid();
        bid.id = "bid-" + impId;
        bid.impid = impId;
        bid.price = 2.0;
        bid.adm = markup;
        bid.crid = "creative-" + impId;
        bid.w = width;
        bid.h = height;
        return bid;
    }

    private static final String NATIVE_JSON =
            "{\"native\":{\"link\":{\"url\":\"https://example.test\"},\"assets\":["
                    + "{\"id\":1,\"title\":{\"text\":\"Instrumentation Native\"}},"
                    + "{\"id\":4,\"data\":{\"value\":\"Valid native body\"}},"
                    + "{\"id\":6,\"data\":{\"value\":\"Open\"}}"
                    + "]}}";

    private static final String VAST_XML =
            "<VAST version=\"4.0\"><Ad id=\"instrumentation\"><InLine>"
                    + "<AdSystem>Apex</AdSystem><AdTitle>Instrumentation Video</AdTitle>"
                    + "<Creatives><Creative><Linear><Duration>00:00:10</Duration>"
                    + "<MediaFiles><MediaFile delivery=\"progressive\" type=\"video/mp4\" width=\"640\" height=\"360\" bitrate=\"500\">"
                    + "https://example.test/video.mp4"
                    + "</MediaFile></MediaFiles></Linear></Creative></Creatives>"
                    + "</InLine></Ad></VAST>";
}
