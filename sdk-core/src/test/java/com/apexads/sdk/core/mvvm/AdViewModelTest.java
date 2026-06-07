package com.apexads.sdk.core.mvvm;

import static com.google.common.truth.Truth.assertThat;

import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;

import org.junit.Before;
import org.junit.Test;

public class AdViewModelTest {

    private AdCache cache;
    private FakeRepository repository;
    private TestViewModel viewModel;
    private RecordingListener listener;

    @Before
    public void setUp() {
        cache = new AdCache();
        repository = new FakeRepository();
        viewModel = new TestViewModel(repository, cache);
        listener = new RecordingListener();
        viewModel.setViewListener(listener);
    }

    @Test
    public void load_cacheHit_usesCachedAdWithoutRepositoryCall() {
        AdData cached = adData("cached");
        cache.put(AdFormat.BANNER, "placement", cached);

        viewModel.load();

        assertThat(repository.calls).isEqualTo(0);
        assertThat(viewModel.getState()).isEqualTo(AdState.LOADED);
        assertThat(viewModel.getAdData()).isEqualTo(cached);
        assertThat(listener.loadedCount).isEqualTo(1);
    }

    @Test
    public void load_whileLoading_isIgnored() {
        viewModel.load();
        viewModel.load();

        assertThat(repository.calls).isEqualTo(1);
        assertThat(viewModel.getState()).isEqualTo(AdState.LOADING);
    }

    @Test
    public void load_success_processesCachesAndNotifiesOnce() {
        AdData ad = adData("live");

        viewModel.load();
        repository.succeed(ad);

        assertThat(viewModel.getState()).isEqualTo(AdState.LOADED);
        assertThat(viewModel.getAdData()).isEqualTo(ad);
        assertThat(cache.get(AdFormat.BANNER, "placement")).isEqualTo(ad);
        assertThat(listener.loadedCount).isEqualTo(1);
        assertThat(listener.failedCount).isEqualTo(0);
    }

    @Test
    public void load_failure_transitionsFailedAndKeepsNoAdData() {
        AdError error = new AdError.Network("timeout", null);

        viewModel.load();
        repository.fail(error);

        assertThat(viewModel.getState()).isEqualTo(AdState.FAILED);
        assertThat(viewModel.getError()).isEqualTo(error);
        assertThat(viewModel.getAdData()).isNull();
        assertThat(listener.failedCount).isEqualTo(1);
    }

    @Test
    public void onAdLoadedFailure_doesNotCacheInvalidCreative() {
        viewModel.throwOnLoad = new AdError.InvalidMarkup("bad markup");

        viewModel.load();
        repository.succeed(adData("bad"));

        assertThat(viewModel.getState()).isEqualTo(AdState.FAILED);
        assertThat(cache.get(AdFormat.BANNER, "placement")).isNull();
        assertThat(listener.failedCount).isEqualTo(1);
        assertThat(listener.loadedCount).isEqualTo(0);
    }

    @Test
    public void destroy_ignoresPendingSuccessAndFailureCallbacks() {
        viewModel.load();
        viewModel.destroy();
        repository.succeed(adData("late-success"));
        repository.fail(new AdError.Network("late-failure", null));

        assertThat(viewModel.getState()).isEqualTo(AdState.IDLE);
        assertThat(viewModel.getAdData()).isNull();
        assertThat(viewModel.getError()).isNull();
        assertThat(cache.size()).isEqualTo(0);
        assertThat(listener.loadedCount).isEqualTo(0);
        assertThat(listener.failedCount).isEqualTo(0);
    }

    @Test
    public void checkAndMarkExpired_evictsAndNotifies() {
        AdData expired = new AdData.Builder()
                .requestId("req").impressionId("imp").bidId("bid")
                .adMarkup("expired").adFormat(AdFormat.BANNER)
                .expiresAt(System.currentTimeMillis() - 1_000L)
                .build();
        viewModel.load();
        repository.succeed(expired);

        assertThat(viewModel.checkAndMarkExpired()).isTrue();

        assertThat(viewModel.getState()).isEqualTo(AdState.EXPIRED);
        assertThat(viewModel.getAdData()).isNull();
        assertThat(cache.get(AdFormat.BANNER, "placement")).isNull();
        assertThat(listener.expiredCount).isEqualTo(1);
    }

    @Test
    public void onDisplayed_clearsAdAndCacheAndNotifies() {
        viewModel.load();
        repository.succeed(adData("show"));

        viewModel.onDisplayed();

        assertThat(viewModel.getState()).isEqualTo(AdState.DISPLAYED);
        assertThat(viewModel.getAdData()).isNull();
        assertThat(cache.get(AdFormat.BANNER, "placement")).isNull();
        assertThat(listener.displayedCount).isEqualTo(1);
    }

    private static AdData adData(String markup) {
        return new AdData.Builder()
                .requestId("req").impressionId("imp").bidId("bid")
                .adMarkup(markup).adFormat(AdFormat.BANNER)
                .width(320).height(50).cpm(1.25).currency("USD")
                .expiresAt(System.currentTimeMillis() + 60_000L)
                .build();
    }

    private static final class TestViewModel extends AdViewModel {
        AdError throwOnLoad;

        TestViewModel(AdRepository repository, AdCache cache) {
            super(repository, cache, AdFormat.BANNER, AdSize.BANNER_320x50, "placement", 0.0);
        }

        @Override
        protected AdData onAdLoaded(AdData adData) throws AdError {
            if (throwOnLoad != null) throw throwOnLoad;
            return adData;
        }
    }

    private static final class FakeRepository implements AdRepository {
        int calls;
        OnSuccess success;
        OnFailure failure;

        @Override
        public void loadAd(
                AdFormat format,
                AdSize size,
                String placementId,
                double bidFloor,
                OnSuccess onSuccess,
                OnFailure onFailure) {
            calls++;
            success = onSuccess;
            failure = onFailure;
        }

        void succeed(AdData data) {
            success.onSuccess(data);
        }

        void fail(AdError error) {
            failure.onFailure(error);
        }
    }

    private static final class RecordingListener implements AdViewModelListener {
        int loadedCount;
        int failedCount;
        int displayedCount;
        int expiredCount;

        @Override
        public void onAdLoaded(AdData adData) {
            loadedCount++;
        }

        @Override
        public void onAdFailed(AdError error) {
            failedCount++;
        }

        @Override
        public void onAdDisplayed() {
            displayedCount++;
        }

        @Override
        public void onAdExpired() {
            expiredCount++;
        }
    }
}
