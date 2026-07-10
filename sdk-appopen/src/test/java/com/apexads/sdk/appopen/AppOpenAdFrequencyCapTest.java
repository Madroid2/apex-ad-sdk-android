package com.apexads.sdk.appopen;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AppOpenAdFrequencyCapTest {

    private Context context;
    private AppOpenAdFrequencyCap cap;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        cap = new AppOpenAdFrequencyCap();
        cap.reset(context);
    }

    @Test
    public void isSatisfied_nonPositiveCapDisablesLimit() {
        cap.record(context);

        assertThat(cap.isSatisfied(context, 0L)).isTrue();
        assertThat(cap.isSatisfied(context, -1L)).isTrue();
    }

    @Test
    public void isSatisfied_firstShowAllowedThenBlockedInsideWindow() {
        assertThat(cap.isSatisfied(context, 60_000L)).isTrue();

        cap.record(context);

        assertThat(cap.isSatisfied(context, 60_000L)).isFalse();
    }

    @Test
    public void reset_clearsRecordedTimestamp() {
        cap.record(context);
        cap.reset(context);

        assertThat(cap.isSatisfied(context, 60_000L)).isTrue();
    }
}
