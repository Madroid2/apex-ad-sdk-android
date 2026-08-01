package com.apexads.sdk.core.request;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apexads.sdk.ApexAdsConfig;
import com.apexads.sdk.BuildConfig;
import com.apexads.sdk.core.audience.AudienceSignals;
import com.apexads.sdk.core.audience.Cohort;
import com.apexads.sdk.core.audience.CohortProvider;
import com.apexads.sdk.core.consent.ConsentManager;
import com.apexads.sdk.core.device.DeviceInfoProvider;
import com.apexads.sdk.core.di.MeasurementDelegate;
import com.apexads.sdk.core.di.WalletDelegate;
import com.apexads.sdk.core.models.AdFormat;
import com.apexads.sdk.core.models.AdSize;
import com.apexads.sdk.core.models.IntentContext;
import com.apexads.sdk.core.models.openrtb.BidRequest;
import com.apexads.sdk.internal.ApexFeatureAccess;
import com.apexads.sdk.internal.ApexSdkRuntime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OpenRTBRequestBuilder {

    /** OpenRTB API framework 7 — OMID-1 (Open Measurement). */
    private static final int API_FRAMEWORK_OMID_1 = 7;

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
    @Nullable private IntentContext intentContext = null;
    @Nullable private String renderSurface = null;
    private boolean declareCapabilities = false;

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
    public OpenRTBRequestBuilder intentContext(@Nullable IntentContext context) { intentContext = context; return this; }

    /**
     * Declares the in-app surface this Native placement renders on (e.g. {@code "assistant"}
     * for a conversational/AI-assistant surface). Carried as {@code imp.ext.apex.surface} so
     * demand can price and copy-fit the creative for the surface. Null omits the field.
     */
    public OpenRTBRequestBuilder renderSurface(@Nullable String surface) {
        renderSurface = surface == null || surface.trim().isEmpty() ? null : surface.trim();
        return this;
    }

    /**
     * Declares the actions this build can actually execute, independent of any
     * {@link IntentContext}. Server-side placement configuration decides which actions are
     * <em>allowed</em>; this list is what the installed modules can <em>perform</em>, and the
     * server forwards only the intersection. Without it a server-configured placement could
     * promise demand a Wallet action that this app has no module to execute.
     */
    public OpenRTBRequestBuilder declareCapabilities(boolean declare) {
        declareCapabilities = declare;
        return this;
    }

    @NonNull
    public BidRequest build() {
        ApexAdsConfig config = ApexSdkRuntime.getConfig();
        DeviceInfoProvider.DeviceInfo device = deviceInfoProvider.getDeviceInfo();

        // Test-mode traffic is synthetic by contract. Never attach GAID/user IDs,
        // even when the emulator or test device has advertising tracking enabled.
        boolean identityAllowed = !config.isTestMode()
                && !device.limitAdTracking
                && config.getCoppa() == 0
                && consentManager.canShareDeviceIdentifiers();

        BidRequest request = new BidRequest();
        request.id = UUID.randomUUID().toString();
        request.imp = Collections.singletonList(buildImpression());
        request.app = buildApp(device);
        request.device = buildDevice(device, identityAllowed);
        request.user = buildUser(device, identityAllowed);
        request.regs = buildRegs();
        request.source = buildSource(request.id, config);
        request.at = BidRequest.AUCTION_FIRST_PRICE;
        request.tmax = (int) config.getRequestTimeoutMs();
        request.test = config.isTestMode() ? 1 : 0;
        request.cur = Collections.singletonList("USD");
        request.bcat = blockedCategories;
        request.badv = blockedDomains;
        request.apexExt = buildApexExt(device);
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
        imp.api = supportedApiFrameworks();

        boolean walletRegistered = ApexFeatureAccess.getFeature(WalletDelegate.class) != null;

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
                Map<String, Object> nativeExt = nativeApexExt(walletRegistered);
                if (nativeExt != null) {
                    imp.ext = nativeExt;
                }
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
        banner.api = supportedApiFrameworks();
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
        if (omidDelegate() != null) {
            video.api = Collections.singletonList(API_FRAMEWORK_OMID_1);
        }
        return video;
    }

    /**
     * Declared API frameworks: MRAID 1/2/3 always; OMID-1 (7) only while a
     * certified measurement module is installed and ready — declaring OMID
     * without live measurement is misrepresentation toward buyers.
     */
    private List<Integer> supportedApiFrameworks() {
        if (omidDelegate() != null) {
            return Arrays.asList(3, 5, 6, API_FRAMEWORK_OMID_1);
        }
        return Arrays.asList(3, 5, 6);
    }

    @Nullable
    private MeasurementDelegate omidDelegate() {
        MeasurementDelegate delegate = ApexFeatureAccess.getFeature(MeasurementDelegate.class);
        return delegate != null && delegate.isReady() ? delegate : null;
    }

    private static Map<String, Object> walletSupportedExt() {
        Map<String, Object> ext = new HashMap<>();
        ext.put("wallet_supported", true);
        return ext;
    }

    /**
     * Builds {@code imp.ext} for Native placements. The apex envelope carries the optional
     * Intent-to-Action context and, for conversational placements, the render surface.
     * Returns null when there is nothing to declare so plain Native requests stay untouched.
     */
    @Nullable
    private Map<String, Object> nativeApexExt(boolean walletRegistered) {
        if (intentContext == null && renderSurface == null && !declareCapabilities) return null;
        Map<String, Object> ext = new HashMap<>();
        Map<String, Object> apex = new HashMap<>();
        if (renderSurface != null) apex.put("surface", renderSurface);
        if (intentContext != null) {
            // Publisher declared intent in-app: forward it, filtered by capability.
            apex.put("intent", intentContext.toOpenRtbMap());
            apex.put("actions_supported", executableActions(intentContext.supportedActions, walletRegistered));
        } else if (declareCapabilities) {
            // Intent is configured server-side. All the SDK contributes is what this
            // build can execute; the server intersects it with placement policy.
            apex.put("actions_supported", installedCapabilities(walletRegistered));
        }
        if (walletRegistered) apex.put("wallet_supported", true);
        ext.put("apex", apex);
        // Keep the existing flat flag for older demand adapters.
        if (walletRegistered) ext.put("wallet_supported", true);
        return ext;
    }

    /** Publisher-declared actions, minus any whose module is not installed. */
    private static List<String> executableActions(
            @NonNull List<IntentContext.ActionType> declared, boolean walletRegistered) {
        List<String> actions = new ArrayList<>(declared.size());
        for (IntentContext.ActionType action : declared) {
            if (action != IntentContext.ActionType.SAVE_TO_WALLET || walletRegistered) {
                actions.add(action.wireValue());
            }
        }
        return actions;
    }

    /** Every action this build can execute, derived from the installed modules. */
    private static List<String> installedCapabilities(boolean walletRegistered) {
        List<String> capabilities = new ArrayList<>(2);
        // A deep link needs no optional module — the core click path handles it.
        capabilities.add(IntentContext.ActionType.OPEN_DEEPLINK.wireValue());
        if (walletRegistered) {
            capabilities.add(IntentContext.ActionType.SAVE_TO_WALLET.wireValue());
        }
        return capabilities;
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
        pub.id = ApexSdkRuntime.getConfig().getAppToken();
        app.publisher = pub;
        return app;
    }

    private BidRequest.Device buildDevice(DeviceInfoProvider.DeviceInfo device,
                                          boolean identityAllowed) {
        BidRequest.Device d = new BidRequest.Device();
        d.ua = device.userAgent;
        d.make = device.manufacturer;
        d.model = device.model;
        d.os = "Android";
        d.osv = device.osVersion;
        d.h = device.screenHeight;
        d.w = device.screenWidth;
        d.ppi = device.ppi > 0 ? device.ppi : null;
        d.pxratio = (double) device.density;
        d.language = device.language;
        d.carrier = device.carrier;
        d.mccmnc = device.mccmnc;
        d.connectiontype = device.connectionType;
        d.js = 1;
        d.devicetype = device.isTablet ? 5 : 4;
        if (identityAllowed) d.ifa = device.advertisingId;
        boolean restrictedTracking = ApexSdkRuntime.getConfig().isTestMode()
                || device.limitAdTracking;
        d.lmt = restrictedTracking ? 1 : 0;
        d.dnt = restrictedTracking ? 1 : 0;
        if (device.geoCountry != null) {
            d.geo = new BidRequest.Geo();
            d.geo.country = device.geoCountry;
        }
        return d;
    }

    private BidRequest.User buildUser(DeviceInfoProvider.DeviceInfo device,
                                      boolean identityAllowed) {
        BidRequest.User user = new BidRequest.User();
        user.id = identityAllowed ? device.advertisingId : null;
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
        CohortProvider provider = ApexSdkRuntime.getCohortProvider();
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

    /**
     * SupplyChain (schain 1.0): the SDK is the first, and for direct app inventory the
     * only, hop between the publisher and the exchange. DSPs filter unauthorized supply
     * on this object, so every request declares a complete single-node chain.
     */
    private BidRequest.Source buildSource(String requestId, ApexAdsConfig config) {
        BidRequest.SupplyChainNode node = new BidRequest.SupplyChainNode();
        node.asi = config.getSupplyChainDomain();
        node.sid = config.getSellerId();
        node.hp = 1;
        node.rid = requestId;

        BidRequest.SupplyChain schain = new BidRequest.SupplyChain();
        schain.complete = 1;
        schain.ver = "1.0";
        schain.nodes = Collections.singletonList(node);

        BidRequest.Source source = new BidRequest.Source();
        source.schain = schain;
        MeasurementDelegate omid = omidDelegate();
        if (omid != null) {
            source.omidpn = omid.partnerName();
            source.omidpv = omid.partnerVersion();
        }
        return source;
    }

    private BidRequest.Regs buildRegs() {
        ApexAdsConfig config = ApexSdkRuntime.getConfig();
        BidRequest.Regs regs = new BidRequest.Regs();
        regs.coppa = config.getCoppa() > 0 ? 1 : null;
        regs.gpp = consentManager.getGppString();
        regs.gpp_sid = consentManager.getGppSectionIds();
        regs.ext = new BidRequest.RegsExt();
        regs.ext.gdpr = consentManager.isGdprApplicable() ? 1 : 0;

        String usPrivacy = config.getUsPrivacyString();
        regs.ext.us_privacy = usPrivacy != null ? usPrivacy : consentManager.getUsPrivacyString();
        return regs;
    }

    private BidRequest.ApexExt buildApexExt(DeviceInfoProvider.DeviceInfo device) {
        ApexAdsConfig config = ApexSdkRuntime.getConfig();
        BidRequest.ApexExt ext = new BidRequest.ApexExt();
        ext.testmode = config.isTestMode() ? 1 : 0;
        ext.gdpr = consentManager.isGdprApplicable() ? 1 : 0;
        ext.tcf = consentManager.getTcfConsentString();

        String usPrivacy = config.getUsPrivacyString();
        ext.ccpa = usPrivacy != null ? usPrivacy : consentManager.getUsPrivacyString();
        ext.deviceRisk = device.deviceRisk;
        ext.emulatorSuspected = device.emulatorSuspected;
        ext.trustSignalsVersion = device.trustSignalsVersion;
        return ext;
    }
}
