package com.apexads.sdk.core.crashreporter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.utils.AdLog;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

            if (previous != null) previous.uncaughtException(thread, throwable);
        });

        installed = true;
        AdLog.i("CrashReporter: installed (endpoint=%s)", dsn.envelopeUrl);
    }

    public static void captureException(@NonNull Throwable throwable) {
        AdLog.w(throwable, "CrashReporter: captureException called");
    }

    private static void report(@NonNull Throwable throwable, @NonNull CrashDelivery delivery) {
        try {
            CrashEvent event = new CrashEvent(throwable);

            deliveryExecutor.submit(() -> delivery.deliver(event));

            Thread.sleep(3_000);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            AdLog.e(e, "CrashReporter: error building crash event");
        }
    }
}
