package com.apexads.sdk.core.audience;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeclarativeCohortProviderTest {

    private static AudienceSignals signals(String language, String country,
                                           String deviceType, int connectionType) {
        Map<String, String> s = new HashMap<>();
        s.put(AudienceSignals.FIELD_LANGUAGE, language);
        s.put(AudienceSignals.FIELD_COUNTRY, country);
        s.put(AudienceSignals.FIELD_DEVICE_TYPE, deviceType);
        Map<String, Double> n = new HashMap<>();
        n.put(AudienceSignals.FIELD_CONNECTION_TYPE, (double) connectionType);
        return AudienceSignals.of(s, n);
    }

    private static List<String> ids(List<Cohort> cohorts) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (Cohort c : cohorts) out.add(c.id());
        return out;
    }

    @Test
    public void resolve_simpleIn_matchesLanguage() {
        String json = "{ \"cohorts\": [ { \"id\": \"de\", \"name\": \"German\", " +
                "\"match\": { \"field\": \"language\", \"op\": \"in\", \"values\": [\"de\"] } } ] }";
        DeclarativeCohortProvider p = DeclarativeCohortProvider.fromJson(json);

        assertThat(ids(p.resolve(signals("de", "DE", "phone", 2)))).containsExactly("de");
        assertThat(p.resolve(signals("en", "US", "phone", 2))).isEmpty();
    }

    @Test
    public void resolve_allGroup_requiresEveryLeaf() {
        String json = "{ \"cohorts\": [ { \"id\": \"wifi_tablet\", \"match\": { \"all\": [" +
                "{ \"field\": \"deviceType\", \"op\": \"eq\", \"value\": \"tablet\" }," +
                "{ \"field\": \"connectionType\", \"op\": \"eq\", \"value\": 2 } ] } } ] }";
        DeclarativeCohortProvider p = DeclarativeCohortProvider.fromJson(json);

        assertThat(ids(p.resolve(signals("en", "US", "tablet", 2)))).containsExactly("wifi_tablet");
        assertThat(p.resolve(signals("en", "US", "tablet", 3))).isEmpty();   // wrong connection
        assertThat(p.resolve(signals("en", "US", "phone", 2))).isEmpty();    // wrong device
    }

    @Test
    public void resolve_anyAndNot_compose() {
        String json = "{ \"cohorts\": [ { \"id\": \"dach_non_wifi\", \"match\": { \"all\": [" +
                "{ \"any\": [ { \"field\": \"country\", \"op\": \"eq\", \"value\": \"DE\" }," +
                "             { \"field\": \"country\", \"op\": \"eq\", \"value\": \"AT\" } ] }," +
                "{ \"not\": { \"field\": \"connectionType\", \"op\": \"eq\", \"value\": 2 } } ] } } ] }";
        DeclarativeCohortProvider p = DeclarativeCohortProvider.fromJson(json);

        assertThat(ids(p.resolve(signals("de", "AT", "phone", 4)))).containsExactly("dach_non_wifi");
        assertThat(p.resolve(signals("de", "AT", "phone", 2))).isEmpty();    // on wifi → excluded
        assertThat(p.resolve(signals("en", "US", "phone", 4))).isEmpty();    // wrong country
    }

    @Test
    public void resolve_numericComparators() {
        String json = "{ \"cohorts\": [ { \"id\": \"cellular\", \"match\": " +
                "{ \"field\": \"connectionType\", \"op\": \"gte\", \"value\": 4 } } ] }";
        DeclarativeCohortProvider p = DeclarativeCohortProvider.fromJson(json);

        assertThat(ids(p.resolve(signals("en", "US", "phone", 5)))).containsExactly("cellular");
        assertThat(p.resolve(signals("en", "US", "phone", 2))).isEmpty();
    }

    @Test
    public void fromJson_skipsMalformedCohortsButKeepsValidOnes() {
        String json = "{ \"cohorts\": [" +
                "{ \"id\": \"good\", \"match\": { \"field\": \"language\", \"op\": \"eq\", \"value\": \"en\" } }," +
                "{ \"name\": \"no-id-no-match\" }" +     // malformed: missing id + match
                "] }";
        DeclarativeCohortProvider p = DeclarativeCohortProvider.fromJson(json);

        assertThat(p.size()).isEqualTo(1);
        assertThat(ids(p.resolve(signals("en", "US", "phone", 2)))).containsExactly("good");
    }

    @Test
    public void fromJson_unparseableDocument_yieldsEmptyProvider() {
        DeclarativeCohortProvider p = DeclarativeCohortProvider.fromJson("not json at all");
        assertThat(p.size()).isEqualTo(0);
        assertThat(p.resolve(signals("en", "US", "phone", 2))).isEmpty();
    }

    @Test
    public void resolve_multipleCohorts_allMatchingReturned() {
        String json = "{ \"cohorts\": [" +
                "{ \"id\": \"a\", \"match\": { \"field\": \"os\", \"op\": \"eq\", \"value\": \"android\" } }," +
                "{ \"id\": \"b\", \"match\": { \"field\": \"deviceType\", \"op\": \"eq\", \"value\": \"phone\" } }" +
                "] }";
        DeclarativeCohortProvider p = DeclarativeCohortProvider.fromJson(json);

        Map<String, String> s = new HashMap<>();
        s.put(AudienceSignals.FIELD_OS, "android");
        s.put(AudienceSignals.FIELD_DEVICE_TYPE, "phone");
        List<Cohort> cohorts = p.resolve(AudienceSignals.of(s, Collections.emptyMap()));
        assertThat(ids(cohorts)).containsExactly("a", "b");
    }

    @Test
    public void none_providerAlwaysEmpty() {
        assertThat(CohortProvider.NONE.resolve(signals("de", "DE", "phone", 2))).isEmpty();
    }
}
