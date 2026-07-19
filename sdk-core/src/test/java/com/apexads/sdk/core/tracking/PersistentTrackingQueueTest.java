package com.apexads.sdk.core.tracking;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public final class PersistentTrackingQueueTest {

    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private final Executor direct = Runnable::run;
    private long nowMs = 1_000_000L;
    private final PersistentTrackingQueue.Clock clock = () -> nowMs;

    private final List<String> sent = new ArrayList<>();
    private boolean transportUp = true;

    private PersistentTrackingQueue.Transport transport;

    @Before
    public void setUp() {
        transport = url -> {
            sent.add(url);
            return transportUp;
        };
    }

    private PersistentTrackingQueue newQueue() {
        return new PersistentTrackingQueue(tmp.getRoot(), transport, direct, null, clock);
    }

    @Test
    public void successfulSendRemovesEvent() {
        PersistentTrackingQueue q = newQueue();
        q.fireTrackingUrl("https://ads.example/win?bid_id=b1");

        assertThat(sent).containsExactly("https://ads.example/win?bid_id=b1");
        assertThat(q.pendingCountForTest()).isEqualTo(0);
    }

    @Test
    public void failedSendStaysQueuedWithBackoff() {
        transportUp = false;
        PersistentTrackingQueue q = newQueue();
        q.fireTrackingUrl("https://ads.example/win?bid_id=b1");

        assertThat(sent).hasSize(1);
        assertThat(q.pendingCountForTest()).isEqualTo(1);

        // Immediately draining again must NOT retry — the event is backed off.
        q.drainForTest();
        assertThat(sent).hasSize(1);

        // After the backoff window it retries.
        nowMs += PersistentTrackingQueue.BASE_BACKOFF_MS + 1;
        q.drainForTest();
        assertThat(sent).hasSize(2);
    }

    @Test
    public void eventsSurviveProcessRestart() {
        transportUp = false;
        PersistentTrackingQueue q1 = newQueue();
        q1.fireTrackingUrl("https://ads.example/win?bid_id=b1");
        q1.fireTrackingUrl("https://ads.example/event?type=complete&bid_id=b1");
        assertThat(q1.pendingCountForTest()).isEqualTo(2);

        // "Process death": a fresh queue instance over the same directory.
        sent.clear();
        transportUp = true;
        nowMs += PersistentTrackingQueue.MAX_BACKOFF_MS;
        PersistentTrackingQueue q2 = newQueue();

        assertThat(sent).containsExactly(
                "https://ads.example/win?bid_id=b1",
                "https://ads.example/event?type=complete&bid_id=b1");
        assertThat(q2.pendingCountForTest()).isEqualTo(0);
    }

    @Test
    public void givesUpAfterMaxAttempts() {
        transportUp = false;
        PersistentTrackingQueue q = newQueue();
        q.fireTrackingUrl("https://ads.example/win?bid_id=b1");

        for (int i = 0; i < PersistentTrackingQueue.MAX_ATTEMPTS + 2; i++) {
            nowMs += PersistentTrackingQueue.MAX_BACKOFF_MS + 1;
            q.drainForTest();
        }

        assertThat(q.pendingCountForTest()).isEqualTo(0);
        assertThat(sent).hasSize(PersistentTrackingQueue.MAX_ATTEMPTS);
    }

    @Test
    public void dropsExpiredEvents() {
        transportUp = false;
        PersistentTrackingQueue q = newQueue();
        q.fireTrackingUrl("https://ads.example/win?bid_id=b1");
        int attemptsSoFar = sent.size();

        nowMs += PersistentTrackingQueue.MAX_AGE_MS + 1;
        q.drainForTest();

        assertThat(q.pendingCountForTest()).isEqualTo(0);
        assertThat(sent).hasSize(attemptsSoFar); // expired events are not retried
    }

    @Test
    public void capsQueueSizeByDroppingOldest() {
        transportUp = false;
        PersistentTrackingQueue q = newQueue();
        for (int i = 0; i < PersistentTrackingQueue.MAX_EVENTS + 5; i++) {
            q.fireTrackingUrl("https://ads.example/e?n=" + i);
        }
        assertThat(q.pendingCountForTest()).isEqualTo(PersistentTrackingQueue.MAX_EVENTS);
    }

    @Test
    public void emptyUrlIgnored() {
        PersistentTrackingQueue q = newQueue();
        q.fireTrackingUrl("");
        assertThat(sent).isEmpty();
        assertThat(q.pendingCountForTest()).isEqualTo(0);
    }
}
