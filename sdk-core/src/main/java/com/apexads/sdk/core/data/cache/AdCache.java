package com.apexads.sdk.core.cache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.apexads.sdk.core.utils.AdLog;

/**
 * Thread-safe in-memory ad cache with TTL-based expiry.
 *
 * Keyed by {@link CacheKey} (format + placement). Expired entries are lazily
 * evicted on access and eagerly on {@link #evictExpired()}.
 */
public final class AdCache {

    private final ConcurrentHashMap<CacheKey, AdData> store = new ConcurrentHashMap<>();

    public void put(@NonNull AdFormat format, @Nullable String placementId, @NonNull AdData ad) {
        store.put(new CacheKey(format, placementId), ad);
        AdLog.d("AdCache: cached %s for placement=%s", format.name(), placementId);
    }

    @Nullable
    public AdData get(@NonNull AdFormat format, @Nullable String placementId) {
        CacheKey key = new CacheKey(format, placementId);
        AdData entry = store.get(key);
        if (entry == null) return null;

        if (entry.isExpired()) {
            store.remove(key);
            AdLog.d("AdCache: evicted expired %s for placement=%s", format.name(), placementId);
            return null;
        }
        return entry;
    }

    public void remove(@NonNull AdFormat format, @Nullable String placementId) {
        store.remove(new CacheKey(format, placementId));
    }

    public void evictExpired() {
        int before = store.size();
        Iterator<Map.Entry<CacheKey, AdData>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().isExpired()) it.remove();
        }
        int evicted = before - store.size();
        if (evicted > 0) AdLog.d("AdCache: evicted %d expired entries", evicted);
    }

    public void clear() { store.clear(); }

    public int size() { return store.size(); }

    static final class CacheKey {
        final AdFormat format;
        final String placementId;

        CacheKey(@NonNull AdFormat format, @Nullable String placementId) {
            this.format = format;
            this.placementId = placementId;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey)) return false;
            CacheKey k = (CacheKey) o;
            return format == k.format && Objects.equals(placementId, k.placementId);
        }

        @Override public int hashCode() {
            return Objects.hash(format, placementId);
        }
    }
}
