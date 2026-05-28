package com.apexads.sdk.core.mvvm;

/**
 * Lifecycle state of an Apex ad unit.
 *
 * <p>The state machine follows the same six-phase model as Smaato's ng-sdk-android
 * {@code AdStatus} enum, with an additional {@link #FAILED} phase so errors are
 * carried through the observable rather than thrown across thread boundaries.
 *
 * <pre>
 *   IDLE ──load()──► LOADING ──auction ok──► LOADED ──show()──► DISPLAYED
 *                        │                      │
 *                    error/no-fill           TTL elapsed
 *                        ▼                      ▼
 *                     FAILED                 EXPIRED
 * </pre>
 */
public enum AdState {

    /** Initial state — no load has been requested yet. */
    IDLE,

    /** Auction in progress — a network call has been dispatched. */
    LOADING,

    /** Auction succeeded — creative is ready to be rendered. */
    LOADED,

    /** Creative is currently rendered in a view. */
    DISPLAYED,

    /** Loaded creative has exceeded its TTL; a fresh load is needed. */
    EXPIRED,

    /** Auction or rendering failed — inspect {@link AdViewModel#getError()}. */
    FAILED
}
