package com.apexads.sdk.core.error;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class AdErrorTest {

    @Test
    public void typedErrorsExposeStableCodesAndMessages() {
        assertThat(new AdError.NoFill().getCode()).isEqualTo(AdError.NO_FILL);
        assertThat(new AdError.NoFill("empty auction").getMessage()).isEqualTo("empty auction");
        assertThat(new AdError.Network("timeout").getCode()).isEqualTo(AdError.NETWORK_ERROR);
        assertThat(new AdError.InvalidMarkup("bad").getCode()).isEqualTo(AdError.INVALID_MARKUP);
        assertThat(new AdError.NotInitialized().getCode()).isEqualTo(AdError.NOT_INITIALIZED);
        assertThat(new AdError.ConsentRequired().getCode()).isEqualTo(AdError.CONSENT_REQUIRED);
        assertThat(new AdError.ConsentRequired("blocked").getMessage()).isEqualTo("blocked");
        assertThat(new AdError.Internal("boom").getCode()).isEqualTo(AdError.INTERNAL);
    }

    @Test
    public void errorsCanPreserveCause() {
        RuntimeException cause = new RuntimeException("root");

        assertThat(new AdError.Network("network", cause).getCause()).isEqualTo(cause);
        assertThat(new AdError.Internal("internal", cause).getCause()).isEqualTo(cause);
    }
}
