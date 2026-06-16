package com.apexads.sdk.core.utils;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class AdNavigationGuardTest {

    @Test
    public void evaluateNavigation_blocksWithoutGesture() {
        AdNavigationGuard guard = new AdNavigationGuard("test");
        guard.reset(null);

        AdNavigationGuard.Decision decision =
                guard.evaluateNavigation("https://example.com", false, true, "test");

        assertThat(decision.allowed).isFalse();
        assertThat(decision.reason).contains("no_gesture");
    }

    @Test
    public void evaluateNavigation_allowsNativeGesture() {
        AdNavigationGuard guard = new AdNavigationGuard("test");
        guard.reset(null);

        AdNavigationGuard.Decision decision =
                guard.evaluateNavigation("https://example.com", true, true, "test");

        assertThat(decision.allowed).isTrue();
        assertThat(decision.safeUrl).isEqualTo("https://example.com");
    }

    @Test
    public void evaluateNavigation_allowsRecentTouchFallback() {
        AdNavigationGuard guard = new AdNavigationGuard("test");
        long now = System.currentTimeMillis();
        guard.resetClockForTest(now);
        guard.recordUserGestureForTest(now);

        AdNavigationGuard.Decision decision =
                guard.evaluateNavigation("https://example.com", false, false, "legacy");

        assertThat(decision.allowed).isTrue();
    }

    @Test
    public void evaluateNavigation_blocksUnsafeUrlEvenWithGesture() {
        AdNavigationGuard guard = new AdNavigationGuard("test");
        guard.reset(null);

        AdNavigationGuard.Decision decision =
                guard.evaluateNavigation("javascript:alert(1)", true, true, "test");

        assertThat(decision.allowed).isFalse();
        assertThat(decision.reason).contains("unsafe_url");
    }
}
