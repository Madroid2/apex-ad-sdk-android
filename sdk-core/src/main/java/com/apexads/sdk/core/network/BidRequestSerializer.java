package com.apexads.sdk.core.network;

import com.apexads.sdk.core.models.openrtb.BidRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Serializes a {@link BidRequest} to an OpenRTB 2.6 JSON string.
 *
 * Uses Android's built-in {@code org.json} — no third-party dependency.
 * JSON field names follow the OpenRTB 2.6 spec (section 3.2.1 et al.).
 */
final class BidRequestSerializer {

    private BidRequestSerializer() {}

    static String serialize(BidRequest req) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", req.id);
        o.put("at", req.at);
        o.put("tmax", req.tmax);
        if (req.test != 0) o.put("test", req.test);
        putStrings(o, "cur", req.cur);
        putStrings(o, "bcat", req.bcat);
        putStrings(o, "badv", req.badv);

        if (req.imp != null && !req.imp.isEmpty()) {
            JSONArray imps = new JSONArray();
            for (BidRequest.Impression imp : req.imp) imps.put(serImp(imp));
            o.put("imp", imps);
        }
        if (req.app    != null) o.put("app",    serApp(req.app));
        if (req.site   != null) o.put("site",   serSite(req.site));
        if (req.device != null) o.put("device", serDevice(req.device));
        if (req.user   != null) o.put("user",   serUser(req.user));
        if (req.regs   != null) o.put("regs",   serRegs(req.regs));
        return o.toString();
    }

    // ── Impression ────────────────────────────────────────────────────────────

    private static JSONObject serImp(BidRequest.Impression imp) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", imp.id);
        o.put("instl", imp.instl);
        o.put("bidfloor", imp.bidfloor);
        o.put("bidfloorcur", imp.bidfloorcur);
        o.put("secure", imp.secure);
        o.putOpt("tagid", imp.tagid);
        o.putOpt("displaymanager", imp.displaymanager);
        o.putOpt("displaymanagerver", imp.displaymanagerver);
        putInts(o, "api", imp.api);
        if (imp.banner       != null) o.put("banner", serBanner(imp.banner));
        if (imp.video        != null) o.put("video",  serVideo(imp.video));
        if (imp.nativeObject != null) o.put("native", serNative(imp.nativeObject)); // "native" JSON key
        if (imp.ext != null && !imp.ext.isEmpty()) o.put("ext", serFlatMap(imp.ext));
        return o;
    }

    // ── Banner ────────────────────────────────────────────────────────────────

    private static JSONObject serBanner(BidRequest.Banner b) throws JSONException {
        JSONObject o = new JSONObject();
        o.putOpt("w", b.w);
        o.putOpt("h", b.h);
        o.putOpt("pos", b.pos);
        putInts(o, "btype", b.btype);
        putInts(o, "battr", b.battr);
        putStrings(o, "mimes", b.mimes);
        putInts(o, "api", b.api);
        if (b.format != null && !b.format.isEmpty()) {
            JSONArray fa = new JSONArray();
            for (BidRequest.Format f : b.format) {
                fa.put(new JSONObject().put("w", f.w).put("h", f.h));
            }
            o.put("format", fa);
        }
        return o;
    }

    // ── Video ─────────────────────────────────────────────────────────────────

    private static JSONObject serVideo(BidRequest.Video v) throws JSONException {
        JSONObject o = new JSONObject();
        putStrings(o, "mimes", v.mimes);
        o.putOpt("minduration", v.minduration);
        o.putOpt("maxduration", v.maxduration);
        putInts(o, "protocols", v.protocols);
        o.putOpt("w", v.w);
        o.putOpt("h", v.h);
        o.putOpt("startdelay", v.startdelay);
        o.putOpt("linearity", v.linearity);
        o.putOpt("skip", v.skip);
        o.putOpt("skipmin", v.skipmin);
        o.putOpt("skipafter", v.skipafter);
        o.putOpt("placement", v.placement);
        putInts(o, "battr", v.battr);
        putInts(o, "api", v.api);
        putInts(o, "playbackmethod", v.playbackmethod);
        return o;
    }

    // ── Native ────────────────────────────────────────────────────────────────

    private static JSONObject serNative(BidRequest.NativeObject n) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("request", n.request);
        o.put("ver", n.ver);
        putInts(o, "api", n.api);
        putInts(o, "battr", n.battr);
        return o;
    }

    // ── App / Site / Publisher ────────────────────────────────────────────────

    private static JSONObject serApp(BidRequest.App a) throws JSONException {
        JSONObject o = new JSONObject();
        o.putOpt("id", a.id);
        o.putOpt("name", a.name);
        o.putOpt("bundle", a.bundle);
        o.putOpt("domain", a.domain);
        o.putOpt("storeurl", a.storeurl);
        o.putOpt("ver", a.ver);
        o.putOpt("privacypolicy", a.privacypolicy);
        putStrings(o, "cat", a.cat);
        if (a.publisher != null) o.put("publisher", serPublisher(a.publisher));
        return o;
    }

    private static JSONObject serSite(BidRequest.Site s) throws JSONException {
        JSONObject o = new JSONObject();
        o.putOpt("id", s.id);
        o.putOpt("name", s.name);
        o.putOpt("domain", s.domain);
        o.putOpt("page", s.page);
        putStrings(o, "cat", s.cat);
        if (s.publisher != null) o.put("publisher", serPublisher(s.publisher));
        return o;
    }

    private static JSONObject serPublisher(BidRequest.Publisher p) throws JSONException {
        JSONObject o = new JSONObject();
        o.putOpt("id", p.id);
        o.putOpt("name", p.name);
        o.putOpt("domain", p.domain);
        putStrings(o, "cat", p.cat);
        return o;
    }

    // ── Device / Geo ──────────────────────────────────────────────────────────

    private static JSONObject serDevice(BidRequest.Device d) throws JSONException {
        JSONObject o = new JSONObject();
        o.putOpt("ua", d.ua);
        o.putOpt("dnt", d.dnt);
        o.putOpt("lmt", d.lmt);
        o.putOpt("ip", d.ip);
        o.putOpt("devicetype", d.devicetype);
        o.putOpt("make", d.make);
        o.putOpt("model", d.model);
        o.putOpt("os", d.os);
        o.putOpt("osv", d.osv);
        o.putOpt("h", d.h);
        o.putOpt("w", d.w);
        o.putOpt("pxratio", d.pxratio);
        o.putOpt("js", d.js);
        o.putOpt("language", d.language);
        o.putOpt("carrier", d.carrier);
        o.putOpt("connectiontype", d.connectiontype);
        o.putOpt("ifa", d.ifa);
        if (d.geo != null) o.put("geo", serGeo(d.geo));
        return o;
    }

    private static JSONObject serGeo(BidRequest.Geo g) throws JSONException {
        JSONObject o = new JSONObject();
        o.putOpt("lat", g.lat);
        o.putOpt("lon", g.lon);
        o.putOpt("type", g.type);
        o.putOpt("country", g.country);
        o.putOpt("region", g.region);
        o.putOpt("city", g.city);
        o.putOpt("zip", g.zip);
        return o;
    }

    // ── User / Regs ───────────────────────────────────────────────────────────

    private static JSONObject serUser(BidRequest.User u) throws JSONException {
        JSONObject o = new JSONObject();
        o.putOpt("id", u.id);
        o.putOpt("yob", u.yob);
        o.putOpt("gender", u.gender);
        o.putOpt("consent", u.consent);
        if (u.ext != null) {
            o.put("ext", new JSONObject().putOpt("consent", u.ext.consent));
        }
        return o;
    }

    private static JSONObject serRegs(BidRequest.Regs r) throws JSONException {
        JSONObject o = new JSONObject();
        o.putOpt("coppa", r.coppa);
        if (r.ext != null) {
            JSONObject ext = new JSONObject();
            ext.putOpt("gdpr", r.ext.gdpr);
            ext.putOpt("us_privacy", r.ext.us_privacy);
            o.put("ext", ext);
        }
        return o;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void putStrings(JSONObject obj, String key, List<String> list)
            throws JSONException {
        if (list == null || list.isEmpty()) return;
        JSONArray a = new JSONArray();
        for (String s : list) a.put(s);
        obj.put(key, a);
    }

    private static void putInts(JSONObject obj, String key, List<Integer> list)
            throws JSONException {
        if (list == null || list.isEmpty()) return;
        JSONArray a = new JSONArray();
        for (int v : list) a.put(v);
        obj.put(key, a);
    }

    /**
     * Serializes a flat {@code Map<String, Object>} to a {@link JSONObject}.
     * Supports {@link Boolean}, {@link Number}, and {@link String} values.
     * Silently skips null values and unsupported types.
     */
    private static JSONObject serFlatMap(Map<String, Object> map) throws JSONException {
        JSONObject o = new JSONObject();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() == null) continue;
            Object v = entry.getValue();
            if (v instanceof Boolean)      o.put(entry.getKey(), (boolean) (Boolean) v);
            else if (v instanceof Integer) o.put(entry.getKey(), (int) (Integer) v);
            else if (v instanceof Long)    o.put(entry.getKey(), (long) (Long) v);
            else if (v instanceof Double)  o.put(entry.getKey(), (double) (Double) v);
            else if (v instanceof String)  o.put(entry.getKey(), (String) v);
        }
        return o;
    }
}
