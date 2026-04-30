package com.apexads.sdk.video.vast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.apexads.sdk.core.utils.AdLog;

/**
 * VAST 4.0 XML parser.
 *
 * Parses media files, tracking events, click URLs, and skip configuration
 * from both Inline and Wrapper VAST documents (single wrapper level).
 *
 * Ref: https://www.iab.com/guidelines/vast/
 */
public final class VastParser {

    // ── Public result types ───────────────────────────────────────────────────

    public enum TrackingEvent {
        START, FIRST_QUARTILE, MIDPOINT, THIRD_QUARTILE, COMPLETE,
        SKIP, MUTE, UNMUTE, PAUSE, RESUME, FULLSCREEN, CLOSE_LINEAR, CREATIVE_VIEW;

        @Nullable
        public static TrackingEvent fromVastName(@NonNull String name) {
            switch (name.toLowerCase()) {
                case "start":           return START;
                case "firstquartile":   return FIRST_QUARTILE;
                case "midpoint":        return MIDPOINT;
                case "thirdquartile":   return THIRD_QUARTILE;
                case "complete":        return COMPLETE;
                case "skip":            return SKIP;
                case "mute":            return MUTE;
                case "unmute":          return UNMUTE;
                case "pause":           return PAUSE;
                case "resume":          return RESUME;
                case "fullscreen":      return FULLSCREEN;
                case "closelinear":     return CLOSE_LINEAR;
                case "creativeview":    return CREATIVE_VIEW;
                default:                return null;
            }
        }
    }

    public static final class MediaFile implements Serializable {
        public final String url;
        public final String type;
        public final int    width;
        public final int    height;
        public final String delivery;
        public final int    bitrate;

        public MediaFile(String url, String type, int width, int height,
                         String delivery, int bitrate) {
            this.url      = url;
            this.type     = type;
            this.width    = width;
            this.height   = height;
            this.delivery = delivery;
            this.bitrate  = bitrate;
        }
    }

    public static final class VastAd implements Serializable {
        public final String               adId;
        public final String               adTitle;
        public final int                  duration;          // seconds
        public final List<MediaFile>      mediaFiles;
        public final List<String>         impressionUrls;
        public final Map<TrackingEvent, List<String>> trackingEvents;
        public final String               clickThroughUrl;
        public final List<String>         clickTrackingUrls;
        public final int                  skipOffset;        // -1 = not skippable
        public final boolean              isWrapper;
        public final String               wrapperAdTagUri;

        VastAd(Builder b) {
            adId              = b.adId;
            adTitle           = b.adTitle;
            duration          = b.duration;
            mediaFiles        = Collections.unmodifiableList(b.mediaFiles);
            impressionUrls    = Collections.unmodifiableList(b.impressionUrls);
            trackingEvents    = Collections.unmodifiableMap(b.trackingEvents);
            clickThroughUrl   = b.clickThroughUrl;
            clickTrackingUrls = Collections.unmodifiableList(b.clickTrackingUrls);
            skipOffset        = b.skipOffset;
            isWrapper         = b.isWrapper;
            wrapperAdTagUri   = b.wrapperAdTagUri;
        }

        /** Returns the highest-bitrate MP4 media file, or null if none available. */
        @Nullable
        public MediaFile getBestMediaFile() {
            MediaFile best = null;
            for (MediaFile mf : mediaFiles) {
                if ("video/mp4".equals(mf.type)) {
                    if (best == null || mf.bitrate > best.bitrate) best = mf;
                }
            }
            return best;
        }

        static final class Builder {
            String adId, adTitle, clickThroughUrl, wrapperAdTagUri;
            int duration = 0, skipOffset = -1;
            boolean isWrapper = false;
            List<MediaFile> mediaFiles        = new ArrayList<>();
            List<String>    impressionUrls    = new ArrayList<>();
            List<String>    clickTrackingUrls = new ArrayList<>();
            Map<TrackingEvent, List<String>> trackingEvents = new EnumMap<>(TrackingEvent.class);
        }
    }

    public static final class VastResult {
        public final VastAd  ad;
        public final String  errorMessage;
        public final boolean isNoFill;

        private VastResult(VastAd ad, String errorMessage, boolean isNoFill) {
            this.ad           = ad;
            this.errorMessage = errorMessage;
            this.isNoFill     = isNoFill;
        }

        public boolean isSuccess() { return ad != null; }

        static VastResult success(VastAd ad)            { return new VastResult(ad, null, false); }
        static VastResult error(String msg)             { return new VastResult(null, msg, false); }
        static VastResult noFill()                      { return new VastResult(null, null, true); }
    }

    // ── Parser ────────────────────────────────────────────────────────────────

    @NonNull
    public VastResult parse(@Nullable String vastXml) {
        if (vastXml == null || vastXml.trim().isEmpty()) {
            return VastResult.error("Empty VAST XML");
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(vastXml)));
            doc.getDocumentElement().normalize();

            NodeList adNodes = doc.getElementsByTagName("Ad");
            if (adNodes.getLength() == 0) return VastResult.noFill();

            Element adEl = (Element) adNodes.item(0);
            String adId  = adEl.getAttribute("id");

            NodeList inlineNodes  = adEl.getElementsByTagName("InLine");
            NodeList wrapperNodes = adEl.getElementsByTagName("Wrapper");

            if (inlineNodes.getLength() > 0) {
                return parseInline(adId.isEmpty() ? null : adId, (Element) inlineNodes.item(0));
            } else if (wrapperNodes.getLength() > 0) {
                return parseWrapper(adId.isEmpty() ? null : adId, (Element) wrapperNodes.item(0));
            }
            return VastResult.noFill();

        } catch (Exception e) {
            AdLog.e(e, "VastParser: failed to parse VAST XML");
            return VastResult.error("XML parse error: " + e.getMessage());
        }
    }

    private VastResult parseInline(String adId, Element node) {
        VastAd.Builder b = new VastAd.Builder();
        b.adId   = adId;
        b.adTitle = textContent(node, "AdTitle");

        NodeList impNodes = node.getElementsByTagName("Impression");
        for (int i = 0; i < impNodes.getLength(); i++) {
            String url = impNodes.item(i).getTextContent().trim();
            if (!url.isEmpty()) b.impressionUrls.add(url);
        }

        Element creative = firstElement(node, "Creative");
        if (creative == null) return VastResult.error("No Creative in InLine VAST");

        Element linear = firstElement(creative, "Linear");
        if (linear == null) return VastResult.error("No Linear in Creative");

        String skipAttr = linear.getAttribute("skipoffset");
        b.skipOffset = skipAttr.isEmpty() ? -1 : parseSkipOffset(skipAttr);
        b.duration   = parseDuration(textContent(linear, "Duration"));

        b.trackingEvents = parseTrackingEvents(linear);
        b.mediaFiles     = parseMediaFiles(linear);

        Element clicks = firstElement(linear, "VideoClicks");
        if (clicks != null) {
            b.clickThroughUrl = textContent(clicks, "ClickThrough");
            NodeList cts = clicks.getElementsByTagName("ClickTracking");
            for (int i = 0; i < cts.getLength(); i++) {
                String url = cts.item(i).getTextContent().trim();
                if (!url.isEmpty()) b.clickTrackingUrls.add(url);
            }
        }

        AdLog.d("VastParser: parsed inline ad=%s duration=%ds media=%d",
                adId, b.duration, b.mediaFiles.size());
        return VastResult.success(new VastAd(b));
    }

    private VastResult parseWrapper(String adId, Element node) {
        String uri = textContent(node, "VASTAdTagURI");
        if (uri == null) return VastResult.error("Wrapper missing VASTAdTagURI");

        VastAd.Builder b = new VastAd.Builder();
        b.adId          = adId;
        b.isWrapper     = true;
        b.wrapperAdTagUri = uri;
        AdLog.d("VastParser: parsed wrapper adId=%s -> %s", adId, uri);
        return VastResult.success(new VastAd(b));
    }

    private Map<TrackingEvent, List<String>> parseTrackingEvents(Element linear) {
        Map<TrackingEvent, List<String>> result = new EnumMap<>(TrackingEvent.class);
        NodeList nodes = linear.getElementsByTagName("Tracking");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el    = (Element) nodes.item(i);
            String event  = el.getAttribute("event");
            String url    = el.getTextContent().trim();
            TrackingEvent te = TrackingEvent.fromVastName(event);
            if (te != null && !url.isEmpty()) {
                result.computeIfAbsent(te, k -> new ArrayList<>()).add(url);
            }
        }
        return result;
    }

    private List<MediaFile> parseMediaFiles(Element linear) {
        List<MediaFile> result = new ArrayList<>();
        NodeList nodes = linear.getElementsByTagName("MediaFile");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el  = (Element) nodes.item(i);
            String url  = el.getTextContent().trim();
            if (url.isEmpty()) continue;
            result.add(new MediaFile(
                    url,
                    el.getAttribute("type"),
                    parseInt(el.getAttribute("width")),
                    parseInt(el.getAttribute("height")),
                    el.getAttribute("delivery"),
                    parseInt(el.getAttribute("bitrate"))
            ));
        }
        return result;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Nullable
    private Element firstElement(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        return nl.getLength() > 0 ? (Element) nl.item(0) : null;
    }

    @Nullable
    private String textContent(Element parent, String tag) {
        Element el = firstElement(parent, tag);
        if (el == null) return null;
        String t = el.getTextContent().trim();
        return t.isEmpty() ? null : t;
    }

    private int parseDuration(@Nullable String hms) {
        if (hms == null) return 0;
        String[] parts = hms.trim().split(":");
        if (parts.length != 3) return 0;
        return parseInt(parts[0]) * 3600 + parseInt(parts[1]) * 60 + parseInt(parts[2]);
    }

    private int parseSkipOffset(@NonNull String value) {
        if (value.contains(":")) return parseDuration(value);
        try { return Integer.parseInt(value.replace("%", "")); }
        catch (NumberFormatException e) { return -1; }
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }
}
