package com.apexads.sdk.core.audience;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A single first-party audience membership resolved on-device.
 *
 * <p>Cohorts are emitted as OpenRTB 2.6 {@code user.data[].segment[]} entries. They are
 * intentionally lightweight identifiers — the heavy modelling (behavioural scoring, ML,
 * lookalike expansion) is the server's job, not the SDK's.</p>
 */
public final class Cohort {

    private final String id;
    @Nullable private final String name;
    @Nullable private final String value;

    public Cohort(@NonNull String id, @Nullable String name, @Nullable String value) {
        this.id = id;
        this.name = name;
        this.value = value;
    }

    public Cohort(@NonNull String id) {
        this(id, null, null);
    }

    @NonNull
    public String id() {
        return id;
    }

    @Nullable
    public String name() {
        return name;
    }

    @Nullable
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cohort)) return false;
        return id.equals(((Cohort) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "Cohort{" + id + "}";
    }
}
