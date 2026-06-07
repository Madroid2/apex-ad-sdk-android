package com.apexads.sdk.core.crashreporter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.utils.AdLog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * In-house crash reporter. Installs a {@link Thread.UncaughtExceptionHandler} that
 * serializes uncaught exceptions and POSTs them to a Sentry-compatible endpoint.
 *
 * No Sentry SDK dependency — uses raw HTTP envelope protocol.
 *
 * Usage (called from ApexAds.init):
 * <pre>
 *     CrashReporter.init("https://key@sentry.io/project");
 * </pre>
 */
public final class CrashReporter {

    private static volatile boolean installed = false;

    private static final ExecutorService deliveryExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "apex-crash-reporter");
                t.setDaemon(true);
                return t;
            });

    private CrashReporter() {}

    public static synchronized void init(@Nullable String sentryDsnString) {
        if (installed || sentryDsnString == null || sentryDsnString.trim().isEmpty()) return;

        SentryDsn dsn;
        try {
            dsn = SentryDsn.parse(sentryDsnString);
        } catch (Exception e) {
            AdLog.e(e, "CrashReporter: invalid Sentry DSN — crash reporting disabled");
            return;
        }

        CrashDelivery delivery = new CrashDelivery(dsn);
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            report(throwable, delivery);
            // Re-invoke the previous handler (system crash dialog / process kill)
            if (previous != null) previous.uncaughtException(thread, throwable);
        });

        installed = true;
        AdLog.i("CrashReporter: installed (endpoint=%s)", dsn.envelopeUrl);
    }

    /** Manually report a non-fatal exception. */
    public static void captureException(@NonNull Throwable throwable) {
        AdLog.w(throwable, "CrashReporter: captureException called");
    }

    private static void report(@NonNull Throwable throwable, @NonNull CrashDelivery delivery) {
        try {
            CrashEvent event = new CrashEvent(throwable);
            // Submit to background thread — UncaughtExceptionHandler must not block the crashing thread
            deliveryExecutor.submit(() -> delivery.deliver(event));
            // Give delivery thread up to 3s before process is killed
            Thread.sleep(3_000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            AdLog.e(e, "CrashReporter: error building crash event");
        }
    }
}
