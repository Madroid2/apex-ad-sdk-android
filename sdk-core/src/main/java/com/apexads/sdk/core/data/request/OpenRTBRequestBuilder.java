package com.apexads.sdk.core.request;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.ApexAds;
import com.apexads.sdk.ApexAdsConfig;
import com.apexads.sdk.BuildConfig;
import com.apexads.sdk.core.audience.AudienceSignals;
import com.apexads.sdk.core.audience.Cohort;
import com.apexads.sdk.core.audience.CohortProvider;
import com.apexads.sdk.core.consent.ConsentManager;
import com.apexads.sdk.core.device.DeviceInfoProvider;
import com.apexads.sdk.core.di.ServiceLocator;
import com.apexads.sdk.core.di.WalletDelegate;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.models.openrtb.BidRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OpenRTBRequestBuilder {

    private static final String APEX_AUDIENCE_TAXONOMY_ID = "apex-audience";
    private static final String APEX_AUDIENCE_TAXONOMY_NAME = "Apex First-Party Cohorts";

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
        request.apexExt = buildApexExt();
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
        imp.api = Arrays.asList(3, 5, 6);

        boolean walletRegistered = ServiceLocator.isRegistered(WalletDelegate.class);

        switch (adFormat) {
            case BANNER:
                imp.banner = buildBanner();

                if (walletRegistered && adSize.height >= 250) {
                    imp.ext = walletSupportedExt();
                }
                break;
            case INTERSTITIAL:
                imp.banner = buildBanner();
                imp.instl = 1;
                if (walletRegistered) {
                    imp.ext = walletSupportedExt();
                }
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
        video.protocols = Arrays.asList(2, 3, 7);
        video.minduration = 5;
        video.maxduration = 60;
        video.skip = 1;
        video.skipafter = 5;
        video.startdelay = 0;
        video.linearity = 1;
        video.playbackmethod = Collections.singletonList(1);
        return video;
    }

    private static Map<String, Object> walletSupportedExt() {
        Map<String, Object> ext = new HashMap<>();
        ext.put("wallet_supported", true);
        return ext;
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
        attachCohorts(user, device);
        return user;
    }

    /**
     * Attaches first-party audience cohorts as OpenRTB {@code user.data[]} segments.
     *
     * <p>Strictly gated on IAB TCF Purpose 4 (personalised ads): without it we send a purely
     * contextual request and the server-side pipeline takes over. Cohort resolution itself is a
     * cheap, in-memory rule match over signals already in the bid request — no extra collection.</p>
     */
    private void attachCohorts(BidRequest.User user, DeviceInfoProvider.DeviceInfo device) {
        if (!consentManager.hasPersonalizationConsent()) {
            return;
        }
        if (!ServiceLocator.isRegistered(CohortProvider.class)) {
            return;
        }
        CohortProvider provider = ServiceLocator.get(CohortProvider.class);
        List<Cohort> cohorts = provider.resolve(AudienceSignals.from(device));
        if (cohorts == null || cohorts.isEmpty()) {
            return;
        }

        List<BidRequest.Segment> segments = new ArrayList<>(cohorts.size());
        for (Cohort c : cohorts) {
            BidRequest.Segment seg = new BidRequest.Segment();
            seg.id = c.id();
            seg.name = c.name();
            seg.value = c.value();
            segments.add(seg);
        }

        BidRequest.Data data = new BidRequest.Data();
        data.id = APEX_AUDIENCE_TAXONOMY_ID;
        data.name = APEX_AUDIENCE_TAXONOMY_NAME;
        data.segment = segments;
        user.data = Collections.singletonList(data);
    }

    private BidRequest.Regs buildRegs() {
        ApexAdsConfig config = ApexAds.getConfig();
        BidRequest.Regs regs = new BidRequest.Regs();
        regs.coppa = config.getCoppa() > 0 ? 1 : null;
        regs.ext = new BidRequest.RegsExt();
        regs.ext.gdpr = consentManager.isGdprApplicable() ? 1 : 0;

        String usPrivacy = config.getUsPrivacyString();
        regs.ext.us_privacy = usPrivacy != null ? usPrivacy : consentManager.getUsPrivacyString();
        return regs;
    }

    private BidRequest.ApexExt buildApexExt() {
        ApexAdsConfig config = ApexAds.getConfig();
        BidRequest.ApexExt ext = new BidRequest.ApexExt();
        ext.testmode = config.isTestMode() ? 1 : 0;
        ext.gdpr = consentManager.isGdprApplicable() ? 1 : 0;
        ext.tcf = consentManager.getTcfConsentString();

        String usPrivacy = config.getUsPrivacyString();
        ext.ccpa = usPrivacy != null ? usPrivacy : consentManager.getUsPrivacyString();
        return ext;
    }
}
