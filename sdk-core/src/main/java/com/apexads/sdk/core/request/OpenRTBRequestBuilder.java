package com.apexads.sdk.core.request;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.ApexAds;
import com.apexads.sdk.ApexAdsConfig;
import com.apexads.sdk.BuildConfig;
import com.apexads.sdk.core.consent.ConsentManager;
import com.apexads.sdk.core.device.DeviceInfoProvider;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.models.openrtb.BidRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Assembles a standards-compliant OpenRTB 2.6 {@link BidRequest}.
 *
 * Call {@link #build()} after configuring the desired format, size, and targeting.
 */
public class OpenRTBRequestBuilder {

    private final DeviceInfoProvider deviceInfoProvider;
    private final ConsentManager consentManager;

    private AdFormat adFormat = AdFormat.BANNER;
    private AdSize adSize = AdSize.BANNER_320x50;
    private String placementId = null;
    private double bidFloor = 0.0;
    private List<String> blockedCategories = null;
    private List<String> blockedDomains = null;

    public OpenRTBRequestBuilder(@NonNull DeviceInfoProvider deviceInfoProvider,
                                 @NonNull ConsentManager consentManager) {
        this.deviceInfoProvider = deviceInfoProvider;
        this.consentManager = consentManager;
    }

    public OpenRTBRequestBuilder adFormat(@NonNull AdFormat format) { adFormat = format; return this; }
    public OpenRTBRequestBuilder adSize(@NonNull AdSize size) { adSize = size; return this; }
    public OpenRTBRequestBuilder placementId(@Nullable String id) { placementId = id; return this; }
    public OpenRTBRequestBuilder bidFloor(double floor) { bidFloor = floor; return this; }
    public OpenRTBRequestBuilder blockedCategories(@NonNull List<String> cats) { blockedCategories = cats; return this; }
    public OpenRTBRequestBuilder blockedDomains(@NonNull List<String> domains) { blockedDomains = domains; return this; }

    @NonNull
    public BidRequest build() {
        ApexAdsConfig config = ApexAds.getConfig();
        DeviceInfoProvider.DeviceInfo device = deviceInfoProvider.getDeviceInfo();

        BidRequest request = new BidRequest();
        request.id = UUID.randomUUID().toString();
        request.imp = Collections.singletonList(buildImpression());
        request.app = buildApp(device);
        request.device = buildDevice(device);
        request.user = buildUser(device);
        request.regs = buildRegs();
        request.at = BidRequest.AUCTION_FIRST_PRICE;
        request.tmax = (int) config.getRequestTimeoutMs();
        request.test = config.isTestMode() ? 1 : 0;
        request.cur = Collections.singletonList("USD");
        request.bcat = blockedCategories;
        request.badv = blockedDomains;
        return request;
    }

    private BidRequest.Impression buildImpression() {
        BidRequest.Impression imp = new BidRequest.Impression();
        imp.id = "1";
        imp.displaymanager = "ApexAdSDK";
        imp.displaymanagerver = BuildConfig.SDK_VERSION;
        imp.bidfloor = bidFloor;
        imp.bidfloorcur = "USD";
        imp.secure = 1;
        imp.tagid = placementId;
        imp.api = Arrays.asList(3, 5, 6); // MRAID 1, 2, 3

        switch (adFormat) {
            case BANNER:
                imp.banner = buildBanner();
                break;
            case INTERSTITIAL:
                imp.banner = buildBanner();
                imp.instl = 1;
                break;
            case REWARDED_VIDEO:
                imp.video = buildVideo();
                break;
            case NATIVE:
                imp.nativeObject = buildNativeObject();
                break;
        }
        return imp;
    }

    private BidRequest.Banner buildBanner() {
        BidRequest.Banner banner = new BidRequest.Banner();
        if (adSize == AdSize.INTERSTITIAL_FULL) {
            banner.format = Arrays.asList(new BidRequest.Format(320, 480), new BidRequest.Format(480, 320));
        } else {
            banner.format = Collections.singletonList(new BidRequest.Format(adSize.width, adSize.height));
            banner.w = adSize.width;
            banner.h = adSize.height;
        }
        banner.api = Arrays.asList(3, 5, 6);
        banner.mimes = Arrays.asList("text/html", "text/javascript");
        return banner;
    }

    private BidRequest.Video buildVideo() {
        BidRequest.Video video = new BidRequest.Video();
        video.mimes = Collections.singletonList("video/mp4");
        video.protocols = Arrays.asList(2, 3, 7); // VAST 2, 3, 4
        video.minduration = 5;
        video.maxduration = 60;
        video.skip = 1;
        video.skipafter = 5;
        video.startdelay = 0;
        video.linearity = 1;
        video.playbackmethod = Collections.singletonList(1);
        return video;
    }

    private BidRequest.NativeObject buildNativeObject() {
        BidRequest.NativeObject nativeObj = new BidRequest.NativeObject();
        nativeObj.request = "{\"ver\":\"1.2\",\"layout\":1,\"adunit\":2," +
            "\"assets\":[{\"id\":1,\"required\":1,\"title\":{\"len\":80}}," +
            "{\"id\":2,\"required\":1,\"img\":{\"type\":3,\"w\":1200,\"h\":627}}," +
            "{\"id\":3,\"img\":{\"type\":1,\"w\":80,\"h\":80}}," +
            "{\"id\":4,\"required\":1,\"data\":{\"type\":2,\"len\":100}}," +
            "{\"id\":5,\"data\":{\"type\":1}}," +
            "{\"id\":6,\"data\":{\"type\":12}}]}";
        nativeObj.ver = "1.2";
        return nativeObj;
    }

    private BidRequest.App buildApp(DeviceInfoProvider.DeviceInfo device) {
        BidRequest.App app = new BidRequest.App();
        app.bundle = device.packageName;
        app.name = device.appName;
        app.ver = device.appVersion;
        BidRequest.Publisher pub = new BidRequest.Publisher();
        pub.id = ApexAds.getConfig().getAppToken();
        app.publisher = pub;
        return app;
    }

    private BidRequest.Device buildDevice(DeviceInfoProvider.DeviceInfo device) {
        BidRequest.Device d = new BidRequest.Device();
        d.ua = device.userAgent;
        d.make = device.manufacturer;
        d.model = device.model;
        d.os = "Android";
        d.osv = device.osVersion;
        d.h = device.screenHeight;
        d.w = device.screenWidth;
        d.pxratio = (double) device.density;
        d.language = device.language;
        d.connectiontype = device.connectionType;
        d.js = 1;
        d.devicetype = device.isTablet ? 5 : 4;
        if (!device.limitAdTracking) d.ifa = device.advertisingId;
        d.lmt = device.limitAdTracking ? 1 : 0;
        d.dnt = device.limitAdTracking ? 1 : 0;
        return d;
    }

    private BidRequest.User buildUser(DeviceInfoProvider.DeviceInfo device) {
        BidRequest.User user = new BidRequest.User();
        user.id = device.advertisingId;
        String tcf = consentManager.getTcfConsentString();
        user.consent = tcf;
        if (tcf != null) {
            user.ext = new BidRequest.UserExt();
            user.ext.consent = tcf;
        }
        return user;
    }

    private BidRequest.Regs buildRegs() {
        ApexAdsConfig config = ApexAds.getConfig();
        BidRequest.Regs regs = new BidRequest.Regs();
        regs.coppa = config.getCoppa() > 0 ? 1 : null;
        regs.ext = new BidRequest.RegsExt();
        regs.ext.gdpr = consentManager.isGdprApplicable() ? 1 : 0;
        regs.ext.us_privacy = config.getUsPrivacyString();
        return regs;
    }
}
