package com.apexads.sdk.core.models.openrtb;

import java.util.List;
import java.util.Map;

/**
 * OpenRTB 2.6 Bid Request top-level object.
 * Ref: https://www.iab.com/wp-content/uploads/2022/04/OpenRTB-2-6_FINAL.pdf
 *
 * Plain POJO — JSON serialization handled by BidRequestSerializer (org.json, no 3p dep).
 */
public class BidRequest {

    public static final int AUCTION_FIRST_PRICE  = 1;
    public static final int AUCTION_SECOND_PRICE = 2;

    public String id;
    public List<Impression> imp;
    public App app;
    public Site site;
    public Device device;
    public User user;
    public Regs regs;
    public int at = AUCTION_FIRST_PRICE;
    public int tmax = 500;
    public List<String> cur;
    public List<String> bcat;
    public List<String> badv;
    public int test = 0;
    public Map<String, Object> ext;

    // ── Impression ───────────────────────────────────────────────────────────

    public static class Impression {
        public String id;
        public Banner banner;
        public Video video;
        /** Serialized as "native" JSON key — Java keyword avoided via field rename. */
        public NativeObject nativeObject;
        public int instl = 0;
        public String tagid;
        public double bidfloor = 0.0;
        public String bidfloorcur = "USD";
        public int secure = 1;
        public List<Integer> api;
        public String displaymanager;
        public String displaymanagerver;
        public Map<String, Object> ext;
    }

    // ── Banner ───────────────────────────────────────────────────────────────

    public static class Banner {
        public List<Format> format;
        public Integer w;
        public Integer h;
        public List<Integer> btype;
        public List<Integer> battr;
        public Integer pos;
        public List<String> mimes;
        public List<Integer> api;
    }

    public static class Format {
        public int w;
        public int h;
        public Format(int w, int h) { this.w = w; this.h = h; }
    }

    // ── Video ────────────────────────────────────────────────────────────────

    public static class Video {
        public List<String> mimes;
        public Integer minduration;
        public Integer maxduration;
        public List<Integer> protocols;
        public Integer w;
        public Integer h;
        public Integer startdelay;
        public Integer linearity;
        public Integer skip;
        public Integer skipmin;
        public Integer skipafter;
        public List<Integer> battr;
        public List<Integer> api;
        public List<Integer> playbackmethod;
        public Integer placement;
    }

    // ── Native ───────────────────────────────────────────────────────────────

    public static class NativeObject {
        public String request; // JSON-encoded native request
        public String ver = "1.2";
        public List<Integer> api;
        public List<Integer> battr;
    }

    // ── App / Site / Publisher ────────────────────────────────────────────────

    public static class App {
        public String id;
        public String name;
        public String bundle;
        public String domain;
        public String storeurl;
        public List<String> cat;
        public String ver;
        public Integer privacypolicy;
        public Publisher publisher;
    }

    public static class Site {
        public String id;
        public String name;
        public String domain;
        public List<String> cat;
        public String page;
        public Publisher publisher;
    }

    public static class Publisher {
        public String id;
        public String name;
        public List<String> cat;
        public String domain;
    }

    // ── Device / Geo ──────────────────────────────────────────────────────────

    public static class Device {
        public String ua;
        public Geo geo;
        public Integer dnt;
        public Integer lmt;
        public String ip;
        public Integer devicetype;
        public String make;
        public String model;
        public String os;
        public String osv;
        public Integer h;
        public Integer w;
        public Double pxratio;
        public Integer js;
        public String language;
        public String carrier;
        public Integer connectiontype;
        public String ifa;
    }

    public static class Geo {
        public Double lat;
        public Double lon;
        public Integer type;
        public String country;
        public String region;
        public String city;
        public String zip;
    }

    // ── User / Regs ───────────────────────────────────────────────────────────

    public static class User {
        public String id;
        public Integer yob;
        public String gender;
        public String consent;
        public UserExt ext;
    }

    public static class UserExt {
        public String consent;
    }

    public static class Regs {
        public Integer coppa;
        public RegsExt ext;
    }

    public static class RegsExt {
        public Integer gdpr;
        public String us_privacy;
    }
}
