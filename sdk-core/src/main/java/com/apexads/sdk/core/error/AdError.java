package com.apexads.sdk.core.error;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Typed error hierarchy surfaced to publishers via ad listener callbacks.
 * Use static nested subclasses to distinguish failure categories.
 */
public abstract class AdError extends Exception {

    public static final int NO_FILL = 100;
    public static final int NETWORK_ERROR = 200;
    public static final int INVALID_MARKUP = 300;
    public static final int NOT_INITIALIZED = 400;
    public static final int CONSENT_REQUIRED = 500;
    public static final int INTERNAL = 999;

    private final int code;

    protected AdError(int code, @NonNull String message) {
        super(message);
        this.code = code;
    }

    protected AdError(int code, @NonNull String message, @Nullable Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() { return code; }

    /** Exchange returned no eligible bids — normal condition, not a fatal error. */
    public static final class NoFill extends AdError {
        public NoFill() { super(NO_FILL, "No fill from ad exchange"); }
        public NoFill(@NonNull String message) { super(NO_FILL, message); }
    }

    /** Network call failed or timed out. */
    public static final class Network extends AdError {
        public Network(@NonNull String message) { super(NETWORK_ERROR, message); }
        public Network(@NonNull String message, @Nullable Throwable cause) { super(NETWORK_ERROR, message, cause); }
    }

    /** Ad markup is malformed or unsupported format. */
    public static final class InvalidMarkup extends AdError {
        public InvalidMarkup(@NonNull String message) { super(INVALID_MARKUP, message); }
    }

    /** SDK used before ApexAds.init() was called. */
    public static final class NotInitialized extends AdError {
        public NotInitialized() { super(NOT_INITIALIZED, "ApexAds.init() must be called before loading ads."); }
    }

    /** User consent missing or blocked by privacy regulation. */
    public static final class ConsentRequired extends AdError {
        public ConsentRequired() { super(CONSENT_REQUIRED, "User consent required for ad serving"); }
        public ConsentRequired(@NonNull String message) { super(CONSENT_REQUIRED, message); }
    }

    /** Unexpected internal failure. */
    public static final class Internal extends AdError {
        public Internal(@NonNull String message) { super(INTERNAL, message); }
        public Internal(@NonNull String message, @Nullable Throwable cause) { super(INTERNAL, message, cause); }
    }
}
