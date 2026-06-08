package com.apexads.sdk.core.presentation.mvvm;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Minimal observable carrier for {@link AdState} changes.
 *
 * <p>Modelled after Smaato ng-sdk-android's {@code ChangeNotifier<T>}:
 * <ul>
 *   <li>Observers are stored as <em>weak references</em> — Activities and Views are
 *       never leaked even if {@link #removeObserver} is not called explicitly.</li>
 *   <li>{@link #addObserver} delivers the <em>current</em> state immediately,
 *       so a late subscriber never misses an already-delivered LOADED event.</li>
 * </ul>
 *
 * <p><strong>Thread safety:</strong> {@link #setState} and {@link #addObserver}/
 * {@link #removeObserver} must be called from the <em>main thread</em> only.
 * {@link AdViewModel} enforces this via {@link com.apexads.sdk.core.network.SdkExecutors#MAIN}.
 */
public final class AdStateObservable {

    private volatile AdState currentState = AdState.IDLE;
    private final List<WeakReference<AdStateObserver>> observers = new ArrayList<>();

    /** Returns the current state without subscribing. */
    @NonNull
    public AdState getState() {
        return currentState;
    }

    /**
     * Subscribes {@code observer} and immediately notifies it with the current state.
     *
     * <p>Must be called from the main thread.
     */
    public void addObserver(@NonNull AdStateObserver observer) {
        pruneDeadRefs();
        observers.add(new WeakReference<>(observer));
        observer.onAdStateChanged(currentState);   // immediate delivery
    }

    /** Unsubscribes a previously added observer. Safe to call if not subscribed. */
    public void removeObserver(@NonNull AdStateObserver observer) {
        Iterator<WeakReference<AdStateObserver>> it = observers.iterator();
        while (it.hasNext()) {
            AdStateObserver ref = it.next().get();
            if (ref == null || ref == observer) {
                it.remove();
            }
        }
    }

    /**
     * Transitions to {@code state} and notifies all live observers.
     *
     * <p>Must be called from the main thread.
     */
    public void setState(@NonNull AdState state) {
        currentState = state;
        // Iterate over a snapshot so observers may safely remove themselves during delivery
        List<WeakReference<AdStateObserver>> snapshot = new ArrayList<>(observers);
        for (WeakReference<AdStateObserver> ref : snapshot) {
            AdStateObserver obs = ref.get();
            if (obs != null) {
                obs.onAdStateChanged(state);
            }
        }
        pruneDeadRefs();
    }

    private void pruneDeadRefs() {
        Iterator<WeakReference<AdStateObserver>> it = observers.iterator();
        while (it.hasNext()) {
            if (it.next().get() == null) {
                it.remove();
            }
        }
    }
}
