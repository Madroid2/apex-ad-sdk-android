package com.apexads.sdk.core.network;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared thread pools for SDK background work.
 *
 * Replaces Kotlin coroutines Dispatchers.IO / Dispatchers.Main for the Java SDK.
 * Publishers have zero visibility into these threads.
 */
public final class SdkExecutors {

    /** General I/O pool — network calls, disk ops. */
    public static final ExecutorService IO = Executors.newCachedThreadPool(
            new NamedThreadFactory("apexad-io"));

    /** Single-thread pool for sequential tasks (e.g. cache writes). */
    public static final ExecutorService SINGLE = Executors.newSingleThreadExecutor(
            new NamedThreadFactory("apexad-single"));

    /** Posts runnables to the Android main (UI) thread. */
    public static final Handler MAIN = new Handler(Looper.getMainLooper());

    private SdkExecutors() {}

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger count = new AtomicInteger(0);

        NamedThreadFactory(String prefix) { this.prefix = prefix; }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, prefix + "-" + count.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }
}
