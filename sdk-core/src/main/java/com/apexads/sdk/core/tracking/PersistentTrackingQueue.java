package com.apexads.sdk.core.tracking;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.apexads.sdk.core.utils.AdLog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * File-backed tracking event queue with retry/backoff.
 *
 * Billing-relevant events (nurl win notices, MRC-viewable burl, click and
 * quartile beacons) must survive radio gaps and process death — a lost
 * billing event is lost publisher revenue and a billing discrepancy on the
 * server side. Events are appended to a queue file before the first send
 * attempt and removed only after the transport confirms delivery; failures
 * retry with exponential backoff until {@link #MAX_ATTEMPTS} or
 * {@link #MAX_AGE_MS} is exceeded.
 *
 * All queue state is confined to the serial executor, so no locks are held
 * during network sends.
 */
public final class PersistentTrackingQueue implements TrackingClient {

    /** Sends one event URL; returns true when the server accepted it. */
    public interface Transport {
        boolean send(@NonNull String url);
    }

    @VisibleForTesting static final int  MAX_EVENTS      = 500;
    @VisibleForTesting static final int  MAX_ATTEMPTS    = 8;
    @VisibleForTesting static final long BASE_BACKOFF_MS = 5_000L;
    @VisibleForTesting static final long MAX_BACKOFF_MS  = 10 * 60_000L;
    @VisibleForTesting static final long MAX_AGE_MS      = 24 * 60 * 60_000L;

    interface Clock {
        long nowMs();
    }

    private static final class Event {
        final String url;
        final long enqueuedAtMs;
        int attempts;
        long nextAttemptAtMs;

        Event(String url, long enqueuedAtMs, int attempts) {
            this.url = url;
            this.enqueuedAtMs = enqueuedAtMs;
            this.attempts = attempts;
        }
    }

    private final File queueFile;
    private final Transport transport;
    private final Executor executor;
    @Nullable private final ScheduledExecutorService scheduler;
    private final Clock clock;

    // Confined to the serial executor.
    private final Deque<Event> pending = new ArrayDeque<>();
    private boolean loaded;
    private boolean redrainScheduled;

    public PersistentTrackingQueue(@NonNull File directory,
                                   @NonNull Transport transport,
                                   @NonNull Executor serialExecutor,
                                   @Nullable ScheduledExecutorService scheduler) {
        this(directory, transport, serialExecutor, scheduler, System::currentTimeMillis);
    }

    @VisibleForTesting
    PersistentTrackingQueue(@NonNull File directory,
                            @NonNull Transport transport,
                            @NonNull Executor serialExecutor,
                            @Nullable ScheduledExecutorService scheduler,
                            @NonNull Clock clock) {
        this.queueFile = new File(directory, "apexads_tracking_queue.dat");
        this.transport = transport;
        this.executor = serialExecutor;
        this.scheduler = scheduler;
        this.clock = clock;
        // Recover events left over from a previous process on startup.
        serialExecutor.execute(() -> {
            loadLocked();
            drainLocked();
        });
    }

    @Override
    public void fireTrackingUrl(@NonNull String url) {
        if (url.isEmpty()) return;
        executor.execute(() -> {
            loadLocked();
            if (pending.size() >= MAX_EVENTS) {
                Event dropped = pending.pollFirst();
                if (dropped != null) {
                    AdLog.w("TrackingQueue: capacity reached — dropped oldest event %s", dropped.url);
                }
            }
            pending.addLast(new Event(url, clock.nowMs(), 0));
            persistLocked();
            drainLocked();
        });
    }

    /** Runs one drain pass on the calling thread. Test hook. */
    @VisibleForTesting
    void drainForTest() {
        loadLocked();
        drainLocked();
    }

    @VisibleForTesting
    int pendingCountForTest() {
        return pending.size();
    }

    // ── executor-confined internals ──────────────────────────────────────────

    private void loadLocked() {
        if (loaded) return;
        loaded = true;
        if (!queueFile.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(queueFile))) {
            String line;
            while ((line = reader.readLine()) != null && pending.size() < MAX_EVENTS) {
                Event e = parseLine(line);
                if (e != null) {
                    pending.addLast(e);
                }
            }
            AdLog.d("TrackingQueue: recovered %d pending event(s)", pending.size());
        } catch (IOException e) {
            AdLog.w(e, "TrackingQueue: could not read queue file");
        }
    }

    private void drainLocked() {
        long now = clock.nowMs();
        List<Event> due = new ArrayList<>();
        for (Event e : pending) {
            if (e.nextAttemptAtMs <= now) {
                due.add(e);
            }
        }

        boolean changed = false;
        for (Event e : due) {
            if (now - e.enqueuedAtMs > MAX_AGE_MS || e.attempts >= MAX_ATTEMPTS) {
                pending.remove(e);
                changed = true;
                AdLog.w("TrackingQueue: giving up on event after %d attempt(s): %s", e.attempts, e.url);
                continue;
            }
            e.attempts++;
            if (transport.send(e.url)) {
                pending.remove(e);
                changed = true;
            } else {
                e.nextAttemptAtMs = clock.nowMs() + backoffMs(e.attempts);
                changed = true;
            }
        }
        if (changed) {
            persistLocked();
        }
        scheduleRedrainLocked();
    }

    private void scheduleRedrainLocked() {
        if (pending.isEmpty() || scheduler == null || redrainScheduled) return;

        long now = clock.nowMs();
        long earliest = Long.MAX_VALUE;
        for (Event e : pending) {
            earliest = Math.min(earliest, e.nextAttemptAtMs);
        }
        long delay = Math.max(1_000L, earliest - now);
        redrainScheduled = true;
        scheduler.schedule(() -> executor.execute(() -> {
            redrainScheduled = false;
            drainLocked();
        }), delay, TimeUnit.MILLISECONDS);
    }

    private void persistLocked() {
        File tmp = new File(queueFile.getPath() + ".tmp");
        try (Writer writer = new FileWriter(tmp)) {
            for (Event e : pending) {
                writer.write(e.attempts + "|" + e.enqueuedAtMs + "|" + e.url + "\n");
            }
        } catch (IOException ex) {
            AdLog.w(ex, "TrackingQueue: could not persist queue");
            return;
        }
        if (!tmp.renameTo(queueFile)) {
            AdLog.w("TrackingQueue: could not replace queue file");
        }
    }

    @Nullable
    private static Event parseLine(@NonNull String line) {
        String[] parts = line.split("\\|", 3);
        if (parts.length != 3 || parts[2].isEmpty()) return null;
        try {
            return new Event(parts[2], Long.parseLong(parts[1]), Integer.parseInt(parts[0]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static long backoffMs(int attempts) {
        long backoff = BASE_BACKOFF_MS << Math.min(attempts - 1, 20);
        return Math.min(backoff, MAX_BACKOFF_MS);
    }
}
