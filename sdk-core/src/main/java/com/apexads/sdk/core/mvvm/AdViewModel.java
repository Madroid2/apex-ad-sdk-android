package com.apexads.sdk.core.mvvm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.error.AdError;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.utils.AdLog;

/**
 * Abstract base ViewModel for all Apex ad formats.
 *
 * <h3>Why not {@code androidx.lifecycle.ViewModel}?</h3>
 * A third-party SDK must never assume the host app's {@code ViewModelStore}
 * ownership model. Extending AndroidX ViewModel forces callers to supply a
 * {@code ViewModelStoreOwner}, creates ambiguous lifecycle coupling when the SDK
 * is used inside a custom View or a Service, and ties SDK internals to a host
 * dependency that publishers may not use.  This mirrors Smaato's ng-sdk-android
 * ({@code SmaatoSdkViewModel}) which makes the same trade-off.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Owns the {@link AdStateObservable} — the single source of truth for state.</li>
 *   <li>Owns the loaded {@link AdData} and the most recent {@link AdError}.</li>
 *   <li>Delegates network I/O to the injected {@link AdRepository}.</li>
 *   <li>Notifies the view layer through the {@link AdViewModelListener} contract.</li>
 *   <li>Manages {@link AdCache} put/get/evict on behalf of the ad unit.</li>
 * </ul>
 *
 * <h3>Subclass extension point</h3>
 * Override {@link #onAdLoaded(AdData)} to post-process the raw auction data
 * (e.g. parse VAST XML or a native JSON payload). Throw {@link AdError} to
 * signal a parse failure — the base class will transition state to
 * {@link AdState#FAILED} and notify the view listener automatically.
 */
public abstract class AdViewModel {

    private static final String TAG = "AdViewModel";

    // ── Observable state ──────────────────────────────────────────────────────
    protected final AdStateObservable stateObservable = new AdStateObservable();

    // ── Dependencies ──────────────────────────────────────────────────────────
    protected final AdRepository repository;
    protected final AdCache cache;

    // ── Ad state ──────────────────────────────────────────────────────────────
    @Nullable protected AdData adData;
    @Nullable protected AdError lastError;

    @Nullable private AdViewModelListener viewListener;
    private int loadGeneration;
    private boolean destroyed;

    // ── Configuration ─────────────────────────────────────────────────────────
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

    // ── View contract ─────────────────────────────────────────────────────────

    /** Attaches the view contract. The listener receives all subsequent state events. */
    public void setViewListener(@Nullable AdViewModelListener listener) {
        this.viewListener = listener;
    }

    /** Observable state carrier — attach raw {@link AdStateObserver}s here. */
    @NonNull
    public AdStateObservable getStateObservable() {
        return stateObservable;
    }

    /** Snapshot of the current state — does not block. */
    @NonNull
    public AdState getState() {
        return stateObservable.getState();
    }

    /** Most recent error, or {@code null} when not in {@link AdState#FAILED}. */
    @Nullable
    public AdError getError() {
        return lastError;
    }

    /** The loaded creative, or {@code null} when not in {@link AdState#LOADED}. */
    @Nullable
    public AdData getAdData() {
        return adData;
    }

    // ── Lifecycle entry points ────────────────────────────────────────────────

    /**
     * Initiates an ad load.
     *
     * <p>If a non-expired entry exists in the cache it is used directly; otherwise a
     * live OpenRTB auction is fired through the repository.
     *
     * <p>Re-entrant safety: calling {@code load()} while already in
     * {@link AdState#LOADING} is a no-op.
     */
    public final void load() {
        if (stateObservable.getState() == AdState.LOADING) {
            AdLog.d(TAG + "[" + placementId + "]: load() while LOADING — ignored");
            return;
        }
        destroyed = false;

        // Cache-hit path
        AdData cached = cache.get(format, placementId);
        if (cached != null) {
            AdLog.d(TAG + "[" + placementId + "]: cache hit");
            applyLoaded(cached);
            return;
        }

        // Live auction path
        final int generation = ++loadGeneration;
        transitionTo(AdState.LOADING);
        repository.loadAd(
                format, defaultSize, placementId, bidFloor,
                // onSuccess — already on main thread
                data -> {
                    if (shouldIgnoreCallback(generation)) return;
                    applyLoaded(data);
                },
                // onFailure — already on main thread
                error -> {
                    if (shouldIgnoreCallback(generation)) return;
                    lastError = error;
                    transitionTo(AdState.FAILED);
                    if (viewListener != null) viewListener.onAdFailed(error);
                }
        );
    }

    /**
     * Marks the ad as displayed.
     *
     * <p>Clears local state to prevent double-show, transitions to
     * {@link AdState#DISPLAYED}, and notifies the view listener.
     * Call this from the format-specific ViewModel after launching the ad surface.
     */
    public void onDisplayed() {
        adData = null;
        cache.remove(format, placementId);
        transitionTo(AdState.DISPLAYED);
        if (viewListener != null) viewListener.onAdDisplayed();
    }

    /**
     * Checks whether the loaded creative has exceeded its TTL.
     *
     * <p>If expired: removes from cache, transitions to {@link AdState#EXPIRED},
     * notifies the view listener, and returns {@code true}. The caller should reload.
     */
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

    /** {@code true} when a non-expired creative is ready to be shown. */
    public boolean isReady() {
        return stateObservable.getState() == AdState.LOADED
                && adData != null
                && !adData.isExpired();
    }

    /** Releases all held state. Call when the ad unit is permanently discarded. */
    public void destroy() {
        destroyed = true;
        loadGeneration++;
        cache.remove(format, placementId);
        adData = null;
        lastError = null;
        viewListener = null;
        transitionTo(AdState.IDLE);
    }

    // ── Extension point ───────────────────────────────────────────────────────

    /**
     * Post-processes raw {@link AdData} after a successful auction.
     *
     * <p>Called on the <strong>main thread</strong> before state transitions to
     * {@link AdState#LOADED}. The default implementation is a pass-through.
     *
     * <p>To signal a parse failure, <em>throw an {@link AdError}</em> — the base
     * class will catch it, transition to {@link AdState#FAILED}, and invoke
     * {@link AdViewModelListener#onAdFailed} automatically.
     *
     * @param  adData Raw creative data from the auction.
     * @return The (possibly mutated) {@link AdData} to store and surface to the view.
     * @throws AdError when format-specific validation or parsing fails.
     */
    @NonNull
    protected AdData onAdLoaded(@NonNull AdData adData) throws AdError {
        return adData;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    /**
     * Invokes {@link #onAdLoaded(AdData)}, catches any thrown {@link AdError},
     * and performs the appropriate state transition.
     */
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
