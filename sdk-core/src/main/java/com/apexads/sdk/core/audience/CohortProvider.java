package com.apexads.sdk.core.audience;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Resolves the set of audience cohorts a user currently belongs to.
 *
 * <p>This is the single seam through which audience data enters the bid request. The
 * default implementation ({@link DeclarativeCohortProvider}) evaluates remote-config
 * supplied JSON rules against {@link AudienceSignals}. A publisher — or a future
 * server-driven enrichment step — can register an alternative implementation via the
 * service locator without touching the request-building path.</p>
 */
public interface CohortProvider {

    /**
     * @param signals cheap, already-collected device signals.
     * @return the cohorts to attach to the bid request; never null, may be empty.
     */
    @NonNull
    List<Cohort> resolve(@NonNull AudienceSignals signals);

    /** A provider that never returns cohorts — used when no rules are configured. */
    CohortProvider NONE = signals -> java.util.Collections.emptyList();
}
