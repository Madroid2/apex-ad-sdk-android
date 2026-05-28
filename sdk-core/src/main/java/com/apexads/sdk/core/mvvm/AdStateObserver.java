package com.apexads.sdk.core.mvvm;

/**
 * Receives {@link AdState} change notifications dispatched by {@link AdStateObservable}.
 *
 * <p>Implementations are called on the main thread. Any UI update is therefore safe
 * without an additional {@code runOnUiThread} dispatch.
 */
public interface AdStateObserver {

    /** Invoked every time the observed ad unit transitions to a new {@link AdState}. */
    void onAdStateChanged(AdState state);
}
