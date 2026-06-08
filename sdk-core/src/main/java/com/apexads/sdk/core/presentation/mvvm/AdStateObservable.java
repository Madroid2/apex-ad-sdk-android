package com.apexads.sdk.core.presentation.mvvm;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AdStateObservable {

    private volatile AdState currentState = AdState.IDLE;
    private final List<WeakReference<AdStateObserver>> observers = new ArrayList<>();

    @NonNull
    public AdState getState() {
        return currentState;
    }

    public void addObserver(@NonNull AdStateObserver observer) {
        pruneDeadRefs();
        observers.add(new WeakReference<>(observer));
        observer.onAdStateChanged(currentState);
    }

    public void removeObserver(@NonNull AdStateObserver observer) {
        Iterator<WeakReference<AdStateObserver>> it = observers.iterator();
        while (it.hasNext()) {
            AdStateObserver ref = it.next().get();
            if (ref == null || ref == observer) {
                it.remove();
            }
        }
    }

    public void setState(@NonNull AdState state) {
        currentState = state;

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
