package com.apexads.sdk.core.crashreporter;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class SentryDsnTest {

    @Test
    public void parse_validDsnWithoutSecret_buildsEnvelopeUrl() {
        SentryDsn dsn = SentryDsn.parse(" https://public@example.sentry.io/12345 ");

        assertThat(dsn.publicKey).isEqualTo("public");
        assertThat(dsn.envelopeUrl).isEqualTo("https://example.sentry.io/api/12345/envelope/");
    }

    @Test
    public void parse_validDsnWithSecretAndPort_usesPublicKeyOnly() {
        SentryDsn dsn = SentryDsn.parse("https://public:secret@example.test:8443/project");

        assertThat(dsn.publicKey).isEqualTo("public");
        assertThat(dsn.envelopeUrl).isEqualTo("https://example.test:8443/api/project/envelope/");
    }

    @Test
    public void parse_missingPublicKeyOrProject_throws() {
        assertThrows(IllegalArgumentException.class, () -> SentryDsn.parse("https://example.test/123"));
        assertThrows(IllegalArgumentException.class, () -> SentryDsn.parse("https://public@example.test/"));
    }
}
