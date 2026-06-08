package com.apexads.sdk.core.error;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    public static final class NoFill extends AdError {
        public NoFill() { super(NO_FILL, "No fill from ad exchange"); }
        public NoFill(@NonNull String message) { super(NO_FILL, message); }
    }

    public static final class Network extends AdError {
        public Network(@NonNull String message) { super(NETWORK_ERROR, message); }
        public Network(@NonNull String message, @Nullable Throwable cause) { super(NETWORK_ERROR, message, cause); }
    }

    public static final class InvalidMarkup extends AdError {
        public InvalidMarkup(@NonNull String message) { super(INVALID_MARKUP, message); }
    }

    public static final class NotInitialized extends AdError {
        public NotInitialized() { super(NOT_INITIALIZED, "ApexAds.init() must be called before loading ads."); }
    }

    public static final class ConsentRequired extends AdError {
        public ConsentRequired() { super(CONSENT_REQUIRED, "User consent required for ad serving"); }
        public ConsentRequired(@NonNull String message) { super(CONSENT_REQUIRED, message); }
    }

    public static final class Internal extends AdError {
        public Internal(@NonNull String message) { super(INTERNAL, message); }
        public Internal(@NonNull String message, @Nullable Throwable cause) { super(INTERNAL, message, cause); }
    }
}
