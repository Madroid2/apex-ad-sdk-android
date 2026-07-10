package com.apexads.sdk.core.presentation.mvvm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.domain.repository.AdRepository;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.network.SdkExecutors;
import com.apexads.sdk.core.utils.AdLog;

public abstract class AdViewModel {

    private static final String TAG = "AdViewModel";

    protected final AdStateObservable stateObservable = new AdStateObservable();

    protected final AdRepository repository;
    protected final AdCache cache;
    protected final Object stateLock = new Object();

    @Nullable protected AdData adData;
    @Nullable protected AdError lastError;

    @Nullable private AdViewModelListener viewListener;
    private int loadGeneration;
    private boolean destroyed;
    private boolean loadInProgress;

    private final AdFormat format;
    private final AdSize defaultSize;
    private final String placementId;
    private final double bidFloor;

    protected AdViewModel(
            @NonNull AdRepository repository,
            @NonNull AdCache cache,
            @NonNull AdFormat format,
            @NonNull AdSize defaultSize,
            @NonNull String placementId,
            double bidFloor) {
        this.repository = repository;
        this.cache = cache;
        this.format = format;
        this.defaultSize = defaultSize;
        this.placementId = placementId;
        this.bidFloor = bidFloor;
    }

    public void setViewListener(@Nullable AdViewModelListener listener) {
        synchronized (stateLock) {
            this.viewListener = listener;
        }
    }

    @NonNull
    public AdStateObservable getStateObservable() {
        return stateObservable;
    }

    @NonNull
    public AdState getState() {
        return stateObservable.getState();
    }

    @Nullable
    public AdError getError() {
        synchronized (stateLock) {
            return destroyed ? null : lastError;
        }
    }

    @Nullable
    public AdData getAdData() {
        synchronized (stateLock) {
            return destroyed ? null : adData;
        }
    }

    public final void load() {
        final int generation;
        synchronized (stateLock) {
            if (loadInProgress) {
                AdLog.d(TAG + "[" + placementId + "]: load() while LOADING — ignored");
                return;
            }

            destroyed = false;
            loadInProgress = true;
            generation = ++loadGeneration;
            adData = null;
            lastError = null;
            onAdClearedLocked();
        }

        AdData cached = cache.get(format, placementId);
        if (cached != null) {
            AdLog.d(TAG + "[" + placementId + "]: cache hit");
            applyLoaded(generation, cached);
            return;
        }

        synchronized (stateLock) {
            if (shouldIgnoreCallbackLocked(generation)) {
                return;
            }
            transitionToLocked(AdState.LOADING);
        }

        try {
            repository.loadAd(
                    format, defaultSize, placementId, bidFloor,
                    data -> {
                        applyLoaded(generation, data);
                    },
                    error -> {
                        applyFailed(generation, error);
                    }
            );
        } catch (Exception e) {
            applyFailed(generation, new AdError.Network(e.getMessage(), e));
        }
    }

    public void onDisplayed() {
        synchronized (stateLock) {
            if (destroyed) return;
            loadInProgress = false;
            if (!shouldRetainAdDataOnDisplay()) {
                adData = null;
                onAdClearedLocked();
            }
            cache.remove(format, placementId);
            transitionToLocked(AdState.DISPLAYED);
            if (viewListener != null) viewListener.onAdDisplayed();
        }
    }

    /**
     * Return true to keep {@code adData} alive after {@link #onDisplayed()} so the creative
     * markup can be re-rendered into a new view after a configuration change (e.g. rotation)
     * without a new network request.  Subclasses override to opt in; default is false (one-shot).
     */
    protected boolean shouldRetainAdDataOnDisplay() {
        return false;
    }

    public boolean checkAndMarkExpired() {
        synchronized (stateLock) {
            if (destroyed || adData == null || !adData.isExpired()) {
                return false;
            }

            cache.remove(format, placementId);
            adData = null;
            loadInProgress = false;
            onAdClearedLocked();
            transitionToLocked(AdState.EXPIRED);
            if (viewListener != null) viewListener.onAdExpired();
            return true;
        }
    }

    public boolean isReady() {
        synchronized (stateLock) {
            return isReadyLocked();
        }
    }

    public final void destroy() {
        final int generation;
        synchronized (stateLock) {
            destroyed = true;
            loadInProgress = false;
            generation = ++loadGeneration; // invalidates any in-flight load callback
            viewListener = null;
            cache.remove(format, placementId);
            stateObservable.clear(); // drop subscribers so they stop receiving broadcasts
            transitionToLocked(AdState.IDLE);
        }

        SdkExecutors.SINGLE.execute(() -> destroyOnBackgroundThread(generation));
    }

    @NonNull
    protected AdData onAdLoaded(@NonNull AdData adData) throws AdError {
        return adData;
    }

    @NonNull
    protected LoadedAd onAdLoadedResult(@NonNull AdData adData) throws AdError {
        return loadedAd(onAdLoaded(adData));
    }

    protected void onAdLoadedCommittedLocked(@NonNull LoadedAd loadedAd) {}

    protected void onAdClearedLocked() {}

    protected void onDestroyLocked() {}

    protected final boolean isReadyLocked() {
        return !destroyed
                && stateObservable.getState() == AdState.LOADED
                && adData != null
                && !adData.isExpired();
    }

    protected final boolean isDestroyedLocked() {
        return destroyed;
    }

    @NonNull
    protected static LoadedAd loadedAd(@NonNull AdData adData) {
        return new LoadedAd(adData, null);
    }

    @NonNull
    protected static LoadedAd loadedAd(@NonNull AdData adData, @Nullable Object payload) {
        return new LoadedAd(adData, payload);
    }

    private void applyLoaded(int generation, @NonNull AdData raw) {
        synchronized (stateLock) {
            if (shouldIgnoreCallbackLocked(generation)) return;
        }

        LoadedAd loaded;
        try {
            loaded = onAdLoadedResult(raw);
        } catch (AdError parseError) {
            applyFailed(generation, parseError);
            return;
        }

        synchronized (stateLock) {
            if (shouldIgnoreCallbackLocked(generation)) return;
            AdData processed = loaded.adData;
            onAdLoadedCommittedLocked(loaded);
            cache.put(format, placementId, processed);
            adData = processed;
            lastError = null;
            loadInProgress = false;
            transitionToLocked(AdState.LOADED);
            if (viewListener != null) viewListener.onAdLoaded(processed);
        }
    }

    private void applyFailed(int generation, @NonNull AdError error) {
        synchronized (stateLock) {
            if (shouldIgnoreCallbackLocked(generation)) return;
            AdLog.w(TAG + "[" + placementId + "]: load failed — " + error.getMessage());
            lastError = error;
            adData = null;
            loadInProgress = false;
            onAdClearedLocked();
            transitionToLocked(AdState.FAILED);
            if (viewListener != null) viewListener.onAdFailed(error);
        }
    }

    private void transitionToLocked(@NonNull AdState next) {
        AdLog.d(TAG + "[" + placementId + "]: " + stateObservable.getState() + " → " + next);
        stateObservable.setState(next);
    }

    private boolean shouldIgnoreCallbackLocked(int generation) {
        return destroyed || generation != loadGeneration;
    }

    private void destroyOnBackgroundThread(int generation) {
        synchronized (stateLock) {
            if (generation != loadGeneration || !destroyed) return;
            adData = null;
            lastError = null;
            onAdClearedLocked();
            onDestroyLocked();
        }
    }

    protected static final class LoadedAd {
        @NonNull public final AdData adData;
        @Nullable public final Object payload;

        private LoadedAd(@NonNull AdData adData, @Nullable Object payload) {
            this.adData = adData;
            this.payload = payload;
        }
    }
}
