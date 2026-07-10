package com.apexads.sdk.core.crashreporter;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class CrashEventTest {

    @Test
    public void constructor_initializesStableEventFields() {
        RuntimeException throwable = new RuntimeException("boom");

        CrashEvent event = new CrashEvent(throwable);

        assertThat(event.eventId).hasLength(32);
        assertThat(event.timestamp).endsWith("Z");
        assertThat(event.platform).isEqualTo("java");
        assertThat(event.level).isEqualTo("fatal");
        assertThat(event.throwable).isEqualTo(throwable);
    }

    @Test
    public void toEnvelope_serializesEscapedExceptionAndStacktrace() {
        RuntimeException throwable = new RuntimeException("quote \" newline\n tab\t");
        throwable.setStackTrace(new StackTraceElement[]{
                new StackTraceElement("com.example.Ad", "load", "Ad.java", 42)
        });
        CrashEvent event = new CrashEvent(throwable);

        String envelope = event.toEnvelope("public-key");

        assertThat(envelope).contains("\"dsn\":\"public-key\"");
        assertThat(envelope).contains("\"type\":\"event\"");
        assertThat(envelope).contains("\"type\":\"java.lang.RuntimeException\"");
        assertThat(envelope).contains("quote \\\" newline\\n tab\\t");
        assertThat(envelope).contains("\"module\":\"com.example.Ad\"");
        assertThat(envelope).contains("\"function\":\"load\"");
        assertThat(envelope).contains("\"lineno\":42");
    }
}
