package com.apexads.sdk.core.presentation.mvvm;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class AdStateObservableTest {

    @Test
    public void addObserver_deliversCurrentStateImmediately() {
        AdStateObservable observable = new AdStateObservable();
        observable.setState(AdState.LOADED);
        RecordingObserver observer = new RecordingObserver();

        observable.addObserver(observer);

        assertThat(observer.states).containsExactly(AdState.LOADED);
    }

    @Test
    public void removeObserver_preventsFutureCallbacks() {
        AdStateObservable observable = new AdStateObservable();
        RecordingObserver observer = new RecordingObserver();
        observable.addObserver(observer);
        observable.removeObserver(observer);

        observable.setState(AdState.FAILED);

        assertThat(observer.states).containsExactly(AdState.IDLE);
    }

    @Test
    public void observerCanRemoveItselfDuringDispatch() {
        AdStateObservable observable = new AdStateObservable();
        RecordingObserver retained = new RecordingObserver();
        AdStateObserver selfRemoving = new AdStateObserver() {
            @Override
            public void onAdStateChanged(AdState state) {
                observable.removeObserver(this);
            }
        };

        observable.addObserver(selfRemoving);
        observable.addObserver(retained);
        observable.setState(AdState.LOADING);
        observable.setState(AdState.LOADED);

        assertThat(retained.states).containsExactly(AdState.IDLE, AdState.LOADING, AdState.LOADED).inOrder();
    }

    @Test
    public void weakObserverReference_isPrunedAndDoesNotLeakCallbacks() throws Exception {
        AdStateObservable observable = new AdStateObservable();
        WeakReference<RecordingObserver> observerRef = addTemporaryObserver(observable);

        for (int i = 0; i < 10 && observerRef.get() != null; i++) {
            System.gc();
            Thread.sleep(20);
        }

        observable.setState(AdState.LOADED);

        assertThat(observerRef.get()).isNull();
    }

    @Test
    public void observerMutation_isThreadSafeDuringConcurrentDispatch() throws Exception {
        AdStateObservable observable = new AdStateObservable();
        ExecutorService executor = Executors.newFixedThreadPool(6);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                for (int j = 0; j < 200; j++) {
                    CountingObserver observer = new CountingObserver();
                    observable.addObserver(observer);
                    observable.setState((j % 2 == 0) ? AdState.LOADING : AdState.LOADED);
                    observable.removeObserver(observer);
                }
                return null;
            }));
        }

        start.countDown();
        for (Future<?> future : futures) {
            future.get(5, TimeUnit.SECONDS);
        }
        executor.shutdownNow();

        observable.setState(AdState.FAILED);
        assertThat(observable.getState()).isEqualTo(AdState.FAILED);
    }

    private static WeakReference<RecordingObserver> addTemporaryObserver(AdStateObservable observable) {
        RecordingObserver observer = new RecordingObserver();
        observable.addObserver(observer);
        return new WeakReference<>(observer);
    }

    private static final class RecordingObserver implements AdStateObserver {
        final List<AdState> states = new ArrayList<>();

        @Override
        public void onAdStateChanged(AdState state) {
            states.add(state);
        }
    }

    private static final class CountingObserver implements AdStateObserver {
        final AtomicInteger count = new AtomicInteger();

        @Override
        public void onAdStateChanged(AdState state) {
            count.incrementAndGet();
        }
    }
}
