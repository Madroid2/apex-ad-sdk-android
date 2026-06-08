package com.apexads.sdk.core.models;

public enum AdSize {
    BANNER_320x50(320, 50, "Banner"),
    MRECT_300x250(300, 250, "Medium Rectangle"),
    LEADERBOARD_728x90(728, 90, "Leaderboard"),
    INTERSTITIAL_FULL(0, 0, "Fullscreen"),
    /** Placeholder for native ads — actual size is determined by the publisher layout. */
    NATIVE(0, 0, "Native");

    public final int width;
    public final int height;
    public final String label;

    AdSize(int width, int height, String label) {
        this.width = width;
        this.height = height;
        this.label = label;
    }
}
