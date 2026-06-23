package com.apexads.sdk.core.audience;

import androidx.annotation.NonNull;

import com.apexads.sdk.core.utils.AdLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Evaluates declarative, remote-config supplied cohort rules against {@link AudienceSignals}.
 *
 * <p>Cohort definitions are <b>data, not code</b>. They arrive as a JSON document and are
 * compiled once into an in-memory boolean tree; evaluating a cohort is then a handful of
 * string/number comparisons. There is deliberately no embedded scripting engine — running
 * remote JavaScript on-device previously caused Gradle/AGP version conflicts for publishers
 * and is a needless attack surface. Anything richer than these primitives belongs server-side.</p>
 *
 * <h3>Rule format</h3>
 * <pre>
 * {
 *   "cohorts": [
 *     { "id": "apex_de_speakers", "name": "German speakers",
 *       "match": { "field": "language", "op": "in", "values": ["de"] } },
 *     { "id": "apex_wifi_tablet",
 *       "match": { "all": [
 *         { "field": "deviceType", "op": "eq", "value": "tablet" },
 *         { "field": "connectionType", "op": "eq", "value": 2 } ] } }
 *   ]
 * }
 * </pre>
 *
 * <p>Match nodes are either a boolean group ({@code all} / {@code any} / {@code not}) or a
 * leaf with a {@code field}, an {@code op}, and a {@code value} or {@code values}.
 * Supported ops: {@code eq, neq, in, nin, prefix, exists, gt, gte, lt, lte}.</p>
 */
public final class DeclarativeCohortProvider implements CohortProvider {

    private final List<CompiledCohort> cohorts;

    private DeclarativeCohortProvider(List<CompiledCohort> cohorts) {
        this.cohorts = cohorts;
    }

    /**
     * Compiles a rules document. Malformed individual cohorts are skipped (and logged) so a
     * single bad entry from remote config can never break ad serving. A completely unparseable
     * document yields an empty provider.
     */
    @NonNull
    public static DeclarativeCohortProvider fromJson(@NonNull String json) {
        List<CompiledCohort> compiled = new ArrayList<>();
        try {
            JSONObject root = new JSONObject(json);
            JSONArray arr = root.optJSONArray("cohorts");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject c = arr.optJSONObject(i);
                    if (c == null) continue;
                    try {
                        String id = c.getString("id");
                        String name = c.optString("name", null);
                        Node match = parseNode(c.getJSONObject("match"));
                        compiled.add(new CompiledCohort(id, name, match));
                    } catch (JSONException e) {
                        AdLog.w("DeclarativeCohortProvider: skipping malformed cohort at index %d", i);
                    }
                }
            }
        } catch (JSONException e) {
            AdLog.w(e, "DeclarativeCohortProvider: unparseable rules document, no cohorts active");
        }
        return new DeclarativeCohortProvider(Collections.unmodifiableList(compiled));
    }

    public int size() {
        return cohorts.size();
    }

    @NonNull
    @Override
    public List<Cohort> resolve(@NonNull AudienceSignals signals) {
        if (cohorts.isEmpty()) return Collections.emptyList();
        List<Cohort> matched = new ArrayList<>();
        for (CompiledCohort c : cohorts) {
            if (c.match.eval(signals)) {
                matched.add(new Cohort(c.id, c.name, null));
            }
        }
        return matched;
    }

    // ---------------------------------------------------------------------------------------
    // Parsing
    // ---------------------------------------------------------------------------------------

    private static Node parseNode(JSONObject o) throws JSONException {
        if (o.has("all")) return new GroupNode(parseChildren(o.getJSONArray("all")), GroupNode.Type.ALL);
        if (o.has("any")) return new GroupNode(parseChildren(o.getJSONArray("any")), GroupNode.Type.ANY);
        if (o.has("not")) return new NotNode(parseNode(o.getJSONObject("not")));
        return parseLeaf(o);
    }

    private static List<Node> parseChildren(JSONArray arr) throws JSONException {
        List<Node> children = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            children.add(parseNode(arr.getJSONObject(i)));
        }
        return children;
    }

    private static Node parseLeaf(JSONObject o) throws JSONException {
        String field = o.getString("field");
        String op = o.optString("op", "eq").toLowerCase();
        List<String> values = new ArrayList<>();
        if (o.has("values")) {
            JSONArray a = o.getJSONArray("values");
            for (int i = 0; i < a.length(); i++) values.add(String.valueOf(a.get(i)));
        } else if (o.has("value")) {
            values.add(String.valueOf(o.get("value")));
        }
        return new LeafNode(field, op, values);
    }

    // ---------------------------------------------------------------------------------------
    // Compiled tree
    // ---------------------------------------------------------------------------------------

    private interface Node {
        boolean eval(AudienceSignals s);
    }

    private static final class GroupNode implements Node {
        enum Type { ALL, ANY }
        private final List<Node> children;
        private final Type type;

        GroupNode(List<Node> children, Type type) {
            this.children = children;
            this.type = type;
        }

        @Override
        public boolean eval(AudienceSignals s) {
            if (type == Type.ALL) {
                for (Node n : children) if (!n.eval(s)) return false;
                return true;
            } else {
                for (Node n : children) if (n.eval(s)) return true;
                return false;
            }
        }
    }

    private static final class NotNode implements Node {
        private final Node child;
        NotNode(Node child) { this.child = child; }
        @Override public boolean eval(AudienceSignals s) { return !child.eval(s); }
    }

    private static final class LeafNode implements Node {
        private final String field;
        private final String op;
        private final List<String> values;

        LeafNode(String field, String op, List<String> values) {
            this.field = field;
            this.op = op;
            this.values = values;
        }

        @Override
        public boolean eval(AudienceSignals s) {
            Double num = s.getNumber(field);
            String str = s.getString(field);
            boolean present = num != null || str != null;
            switch (op) {
                case "exists":
                    return present;
                case "eq":
                case "in":
                    return matchesAny(num, str);
                case "neq":
                case "nin":
                    return present && !matchesAny(num, str);
                case "prefix":
                    return str != null && anyPrefix(str);
                case "gt":
                case "gte":
                case "lt":
                case "lte":
                    return compareNumeric(num);
                default:
                    AdLog.w("DeclarativeCohortProvider: unknown op '%s' on field '%s'", op, field);
                    return false;
            }
        }

        private boolean matchesAny(Double num, String str) {
            for (String v : values) {
                if (num != null) {
                    Double target = tryParse(v);
                    if (target != null && num.doubleValue() == target.doubleValue()) return true;
                }
                if (str != null && str.equalsIgnoreCase(v)) return true;
            }
            return false;
        }

        private boolean anyPrefix(String str) {
            for (String v : values) {
                if (str.regionMatches(true, 0, v, 0, v.length())) return true;
            }
            return false;
        }

        private boolean compareNumeric(Double actual) {
            if (actual == null || values.isEmpty()) return false;
            Double target = tryParse(values.get(0));
            if (target == null) return false;
            int cmp = Double.compare(actual, target);
            switch (op) {
                case "gt":  return cmp > 0;
                case "gte": return cmp >= 0;
                case "lt":  return cmp < 0;
                case "lte": return cmp <= 0;
                default:    return false;
            }
        }

        private static Double tryParse(String v) {
            try {
                return Double.parseDouble(v);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    private static final class CompiledCohort {
        final String id;
        final String name;
        final Node match;
        CompiledCohort(String id, String name, Node match) {
            this.id = id;
            this.name = name;
            this.match = match;
        }
    }
}
