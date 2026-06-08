package com.apexads.sdk.core.presentation.mvvm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.domain.repository.AdRepository;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.utils.AdLog;

public abstract class AdViewModel {

    private static final String TAG = "AdViewModel";

    protected final AdStateObservable stateObservable = new AdStateObservable();

    protected final AdRepository repository;
    protected final AdCache cache;

    @Nullable protected AdData adData;
    @Nullable protected AdError lastError;

    @Nullable private AdViewModelListener viewListener;
    private int loadGeneration;
    private boolean destroyed;

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
        this.viewListener = listener;
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
        return lastError;
    }

    @Nullable
    public AdData getAdData() {
        return adData;
    }

    public final void load() {
        if (stateObservable.getState() == AdState.LOADING) {
            AdLog.d(TAG + "[" + placementId + "]: load() while LOADING — ignored");
            return;
        }
        destroyed = false;

        AdData cached = cache.get(format, placementId);
        if (cached != null) {
            AdLog.d(TAG + "[" + placementId + "]: cache hit");
            applyLoaded(cached);
            return;
        }

        final int generation = ++loadGeneration;
        transitionTo(AdState.LOADING);
        repository.loadAd(
                format, defaultSize, placementId, bidFloor,
                data -> {
                    if (shouldIgnoreCallback(generation)) return;
                    applyLoaded(data);
                },
                error -> {
                    if (shouldIgnoreCallback(generation)) return;
                    lastError = error;
                    transitionTo(AdState.FAILED);
                    if (viewListener != null) viewListener.onAdFailed(error);
                }
        );
    }

    public void onDisplayed() {
        adData = null;
        cache.remove(format, placementId);
        transitionTo(AdState.DISPLAYED);
        if (viewListener != null) viewListener.onAdDisplayed();
    }

    public boolean checkAndMarkExpired() {
        if (adData != null && adData.isExpired()) {
            cache.remove(format, placementId);
            adData = null;
            transitionTo(AdState.EXPIRED);
            if (viewListener != null) viewListener.onAdExpired();
            return true;
        }
        return false;
    }

    public boolean isReady() {
        return stateObservable.getState() == AdState.LOADED
                && adData != null
                && !adData.isExpired();
    }

    public void destroy() {
        destroyed = true;
        loadGeneration++; // invalidates any in-flight load callback
        cache.remove(format, placementId);
        adData = null;
        lastError = null;
        viewListener = null;
        stateObservable.clear(); // drop subscribers so they stop receiving broadcasts
        transitionTo(AdState.IDLE);
    }

    @NonNull
    protected AdData onAdLoaded(@NonNull AdData adData) throws AdError {
        return adData;
    }

    private void applyLoaded(@NonNull AdData raw) {
        try {
            AdData processed = onAdLoaded(raw);
            cache.put(format, placementId, processed);
            adData = processed;
            transitionTo(AdState.LOADED);
            if (viewListener != null) viewListener.onAdLoaded(processed);
        } catch (AdError parseError) {
            AdLog.w(TAG + "[" + placementId + "]: onAdLoaded threw " + parseError.getMessage());
            lastError = parseError;
            transitionTo(AdState.FAILED);
            if (viewListener != null) viewListener.onAdFailed(parseError);
        }
    }

    private void transitionTo(@NonNull AdState next) {
        AdLog.d(TAG + "[" + placementId + "]: " + stateObservable.getState() + " → " + next);
        stateObservable.setState(next);
    }

    private boolean shouldIgnoreCallback(int generation) {
        return destroyed || generation != loadGeneration;
    }
}
