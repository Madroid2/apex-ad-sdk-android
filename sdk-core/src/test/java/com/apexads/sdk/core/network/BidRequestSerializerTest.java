package com.apexads.sdk.core.network;

import static com.google.common.truth.Truth.assertThat;

import com.apexads.sdk.core.models.openrtb.BidRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BidRequestSerializerTest {

    @Test
    public void serialize_fullRequest_writesOpenRtbShape() throws Exception {
        BidRequest request = new BidRequest();
        request.id = "req-1";
        request.at = BidRequest.AUCTION_SECOND_PRICE;
        request.tmax = 900;
        request.test = 1;
        request.cur = Arrays.asList("USD", "EUR");
        request.bcat = Collections.singletonList("IAB25");
        request.badv = Collections.singletonList("blocked.example");
        request.ext = new HashMap<>();
        request.ext.put("debug", true);
        request.apexExt = new BidRequest.ApexExt();
        request.apexExt.testmode = 1;
        request.apexExt.gdpr = 1;
        request.apexExt.tcf = "TCF";
        request.apexExt.ccpa = "1YNN";
        request.imp = Collections.singletonList(impression());
        request.app = app();
        request.device = device();
        request.user = user();
        request.regs = regs();

        JSONObject json = new JSONObject(BidRequestSerializer.serialize(request));

        assertThat(json.getString("id")).isEqualTo("req-1");
        assertThat(json.getInt("at")).isEqualTo(2);
        assertThat(json.getInt("tmax")).isEqualTo(900);
        assertThat(json.getInt("test")).isEqualTo(1);
        assertThat(json.getJSONArray("cur").getString(0)).isEqualTo("USD");
        assertThat(json.getJSONArray("bcat").getString(0)).isEqualTo("IAB25");
        assertThat(json.getJSONArray("badv").getString(0)).isEqualTo("blocked.example");
        assertThat(json.getJSONObject("ext").getBoolean("debug")).isTrue();
        assertThat(json.getJSONObject("ext").getJSONObject("apex").getString("tcf")).isEqualTo("TCF");

        JSONObject imp = json.getJSONArray("imp").getJSONObject(0);
        assertThat(imp.getString("id")).isEqualTo("imp-1");
        assertThat(imp.getJSONObject("banner").getJSONArray("format").getJSONObject(0).getInt("w")).isEqualTo(320);
        assertThat(imp.getJSONObject("video").getJSONArray("mimes").getString(0)).isEqualTo("video/mp4");
        assertThat(imp.getJSONObject("native").getString("request")).contains("\"assets\"");
        assertThat(imp.getJSONObject("ext").getString("placement_type")).isEqualTo("test");

        assertThat(json.getJSONObject("app").getString("bundle")).isEqualTo("com.example.app");
        assertThat(json.getJSONObject("device").getString("ifa")).isEqualTo("gaid");
        assertThat(json.getJSONObject("user").getJSONObject("ext").getString("consent")).isEqualTo("TCF");
        assertThat(json.getJSONObject("regs").getJSONObject("ext").getString("us_privacy")).isEqualTo("1YNN");
    }

    @Test
    public void serialize_omitsEmptyOptionalArraysAndExt() throws Exception {
        BidRequest request = new BidRequest();
        request.id = "req-1";
        request.cur = Collections.emptyList();
        request.bcat = null;

        JSONObject json = new JSONObject(BidRequestSerializer.serialize(request));

        assertThat(json.has("cur")).isFalse();
        assertThat(json.has("bcat")).isFalse();
        assertThat(json.has("ext")).isFalse();
        assertThat(json.has("imp")).isFalse();
    }

    @Test
    public void serialize_userData_writesOpenRtbSegments() throws Exception {
        BidRequest request = new BidRequest();
        request.id = "req-1";

        BidRequest.User user = new BidRequest.User();
        user.id = "u1";
        BidRequest.Data data = new BidRequest.Data();
        data.id = "apex-audience";
        data.name = "Apex First-Party Cohorts";
        BidRequest.Segment s1 = new BidRequest.Segment();
        s1.id = "de_speakers";
        s1.name = "German speakers";
        BidRequest.Segment s2 = new BidRequest.Segment();
        s2.id = "wifi_tablet";
        data.segment = Arrays.asList(s1, s2);
        user.data = Collections.singletonList(data);
        request.user = user;

        JSONObject json = new JSONObject(BidRequestSerializer.serialize(request));
        JSONObject dataObj = json.getJSONObject("user").getJSONArray("data").getJSONObject(0);
        assertThat(dataObj.getString("id")).isEqualTo("apex-audience");
        assertThat(dataObj.getJSONArray("segment").getJSONObject(0).getString("id")).isEqualTo("de_speakers");
        assertThat(dataObj.getJSONArray("segment").getJSONObject(1).getString("id")).isEqualTo("wifi_tablet");
    }

    @Test
    public void serialize_userWithoutData_omitsDataArray() throws Exception {
        BidRequest request = new BidRequest();
        request.id = "req-1";
        BidRequest.User user = new BidRequest.User();
        user.id = "u1";
        request.user = user;

        JSONObject json = new JSONObject(BidRequestSerializer.serialize(request));
        assertThat(json.getJSONObject("user").has("data")).isFalse();
    }

    @Test
    public void serialize_source_writesSupplyChainInBothLocations() throws Exception {
        BidRequest request = new BidRequest();
        request.id = "req-1";
        BidRequest.SupplyChainNode node = new BidRequest.SupplyChainNode();
        node.asi = "apexads.net";
        node.sid = "seller-1";
        node.hp = 1;
        node.rid = "req-1";
        BidRequest.SupplyChain schain = new BidRequest.SupplyChain();
        schain.complete = 1;
        schain.ver = "1.0";
        schain.nodes = Collections.singletonList(node);
        request.source = new BidRequest.Source();
        request.source.schain = schain;

        JSONObject json = new JSONObject(BidRequestSerializer.serialize(request));
        JSONObject source = json.getJSONObject("source");

        // OpenRTB 2.6 first-class location.
        JSONObject sc = source.getJSONObject("schain");
        assertThat(sc.getInt("complete")).isEqualTo(1);
        assertThat(sc.getString("ver")).isEqualTo("1.0");
        JSONObject n = sc.getJSONArray("nodes").getJSONObject(0);
        assertThat(n.getString("asi")).isEqualTo("apexads.net");
        assertThat(n.getString("sid")).isEqualTo("seller-1");
        assertThat(n.getInt("hp")).isEqualTo(1);
        assertThat(n.getString("rid")).isEqualTo("req-1");

        // 2.5-compat mirror.
        assertThat(source.getJSONObject("ext").getJSONObject("schain")
                .getJSONArray("nodes").getJSONObject(0).getString("asi"))
                .isEqualTo("apexads.net");
    }

    @Test
    public void serialize_sourceOmidIdentifiers_writtenUnderExt() throws Exception {
        BidRequest request = new BidRequest();
        request.id = "req-1";
        request.source = new BidRequest.Source();
        request.source.omidpn = "Apexads";
        request.source.omidpv = "1.5.0";

        JSONObject json = new JSONObject(BidRequestSerializer.serialize(request));
        JSONObject ext = json.getJSONObject("source").getJSONObject("ext");

        assertThat(ext.getString("omidpn")).isEqualTo("Apexads");
        assertThat(ext.getString("omidpv")).isEqualTo("1.5.0");
        assertThat(json.getJSONObject("source").has("schain")).isFalse();
    }

    @Test
    public void serialize_sourceWithoutOmidOrSchain_omitsExt() throws Exception {
        BidRequest request = new BidRequest();
        request.id = "req-1";
        request.source = new BidRequest.Source();
        request.source.tid = "t-1";

        JSONObject json = new JSONObject(BidRequestSerializer.serialize(request));

        assertThat(json.getJSONObject("source").getString("tid")).isEqualTo("t-1");
        assertThat(json.getJSONObject("source").has("ext")).isFalse();
    }

    @Test
    public void serialize_deviceCarrierFields_written() throws Exception {
        BidRequest request = new BidRequest();
        request.id = "req-1";
        BidRequest.Device device = new BidRequest.Device();
        device.carrier = "TestCarrier";
        device.mccmnc = "310-260";
        device.ppi = 420;
        request.device = device;

        JSONObject json = new JSONObject(BidRequestSerializer.serialize(request));
        JSONObject deviceJson = json.getJSONObject("device");

        assertThat(deviceJson.getString("carrier")).isEqualTo("TestCarrier");
        assertThat(deviceJson.getString("mccmnc")).isEqualTo("310-260");
        assertThat(deviceJson.getInt("ppi")).isEqualTo(420);
    }

    @Test
    public void accessWrapper_wrapsSerialization() {
        BidRequest request = new BidRequest();
        request.id = "req-1";

        assertThat(BidRequestSerializerAccess.serialize(request)).contains("\"id\":\"req-1\"");
    }

    private static BidRequest.Impression impression() {
        BidRequest.Impression imp = new BidRequest.Impression();
        imp.id = "imp-1";
        imp.instl = 1;
        imp.bidfloor = 1.25;
        imp.tagid = "placement";
        imp.api = Arrays.asList(3, 5, 6);
        imp.banner = new BidRequest.Banner();
        imp.banner.w = 320;
        imp.banner.h = 50;
        imp.banner.format = Collections.singletonList(new BidRequest.Format(320, 50));
        imp.banner.api = Collections.singletonList(6);
        imp.banner.mimes = Collections.singletonList("text/html");
        imp.video = new BidRequest.Video();
        imp.video.mimes = Collections.singletonList("video/mp4");
        imp.video.protocols = Collections.singletonList(7);
        imp.video.w = 640;
        imp.video.h = 360;
        imp.nativeObject = new BidRequest.NativeObject();
        imp.nativeObject.request = "{\"assets\":[]}";
        imp.nativeObject.ver = "1.2";
        Map<String, Object> ext = new HashMap<>();
        ext.put("placement_type", "test");
        imp.ext = ext;
        return imp;
    }

    private static BidRequest.App app() {
        BidRequest.App app = new BidRequest.App();
        app.id = "app-id";
        app.name = "App";
        app.bundle = "com.example.app";
        app.cat = Collections.singletonList("IAB1");
        app.publisher = new BidRequest.Publisher();
        app.publisher.id = "pub";
        return app;
    }

    private static BidRequest.Device device() {
        BidRequest.Device device = new BidRequest.Device();
        device.ua = "UA";
        device.lmt = 0;
        device.devicetype = 4;
        device.make = "Google";
        device.model = "Pixel";
        device.os = "Android";
        device.h = 1920;
        device.w = 1080;
        device.pxratio = 2.5;
        device.ifa = "gaid";
        device.geo = new BidRequest.Geo();
        device.geo.country = "US";
        return device;
    }

    private static BidRequest.User user() {
        BidRequest.User user = new BidRequest.User();
        user.id = "user";
        user.consent = "TCF";
        user.ext = new BidRequest.UserExt();
        user.ext.consent = "TCF";
        return user;
    }

    private static BidRequest.Regs regs() {
        BidRequest.Regs regs = new BidRequest.Regs();
        regs.coppa = 1;
        regs.ext = new BidRequest.RegsExt();
        regs.ext.gdpr = 1;
        regs.ext.us_privacy = "1YNN";
        return regs;
    }
}
