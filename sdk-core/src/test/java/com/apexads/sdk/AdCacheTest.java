package com.apexads.sdk;

import static com.google.common.truth.Truth.assertThat;

import com.apexads.sdk.core.cache.AdCache;
import com.apexads.sdk.core.models.AdData;
import com.apexads.sdk.core.models.AdFormat;

import org.junit.Before;
import org.junit.Test;

public class AdCacheTest {

    private AdCache cache;

    @Before
    public void setUp() {
        cache = new AdCache();
    }

    @Test
    public void put_and_get_returns_stored_ad() {
        AdData ad = buildAdData(300);
        cache.put(AdFormat.BANNER, "p1", ad);
        assertThat(cache.get(AdFormat.BANNER, "p1")).isEqualTo(ad);
    }

    @Test
    public void get_returns_null_for_unknown_placement() {
        assertThat(cache.get(AdFormat.BANNER, "unknown")).isNull();
    }

    @Test
    public void get_evicts_and_returns_null_for_expired_entry() {
        AdData expired = buildAdData(-1);
        cache.put(AdFormat.BANNER, "expired", expired);
        assertThat(cache.get(AdFormat.BANNER, "expired")).isNull();
        assertThat(cache.size()).isEqualTo(0);
    }

    @Test
    public void different_formats_keyed_independently() {
        AdData banner = buildAdData(300);
        AdData native_ = buildAdData(300);
        cache.put(AdFormat.BANNER, null, banner);
        cache.put(AdFormat.NATIVE, null, native_);
        assertThat(cache.get(AdFormat.BANNER, null)).isEqualTo(banner);
        assertThat(cache.get(AdFormat.NATIVE, null)).isEqualTo(native_);
        assertThat(cache.size()).isEqualTo(2);
    }

    @Test
    public void remove_deletes_entry() {
        cache.put(AdFormat.BANNER, "p1", buildAdData(300));
        cache.remove(AdFormat.BANNER, "p1");
        assertThat(cache.get(AdFormat.BANNER, "p1")).isNull();
    }

    @Test
    public void evictExpired_removes_only_stale_entries() {
        cache.put(AdFormat.BANNER,       "fresh",   buildAdData(300));
        cache.put(AdFormat.INTERSTITIAL, "stale",   buildAdData(-1));
        cache.put(AdFormat.NATIVE,       "fresh2",  buildAdData(60));
        cache.evictExpired();
        assertThat(cache.size()).isEqualTo(2);
        assertThat(cache.get(AdFormat.BANNER, "fresh")).isNotNull();
        assertThat(cache.get(AdFormat.INTERSTITIAL, "stale")).isNull();
    }

    @Test
    public void clear_empties_all_entries() {
        for (int i = 0; i < 5; i++) {
            cache.put(AdFormat.BANNER, "p" + i, buildAdData(300));
        }
        cache.clear();
        assertThat(cache.size()).isEqualTo(0);
    }

    private AdData buildAdData(long ttlSeconds) {
        return new AdData.Builder()
                .requestId("req-001").impressionId("imp-001").bidId("bid-001")
                .adMarkup("<html/>").adFormat(AdFormat.BANNER)
                .width(320).height(50).cpm(1.50).currency("USD")
                .expiresAt(System.currentTimeMillis() + ttlSeconds * 1000L)
                .build();
    }
}
