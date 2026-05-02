# ApexAd SDK — Android

> A production-grade programmatic advertising SDK demonstrating full-stack ad-tech engineering:
> OpenRTB 2.6 · VAST 4.0 · MRAID 3.0 · IAB Native 1.2 · IAB TCF 2.0 · App Open Ads · In-App Bidding · AdMob Mediation · Google Wallet Pass Ads · In-House Crash Reporting · Custom DI · MRC Viewability

[![API](https://img.shields.io/badge/API-21%2B-brightgreen)](https://android-arsenal.com/api?level=21)
[![OpenRTB](https://img.shields.io/badge/OpenRTB-2.6-orange)](https://www.iab.com/guidelines/openrtb/)
[![VAST](https://img.shields.io/badge/VAST-4.0-red)](https://www.iab.com/guidelines/vast/)
[![MRAID](https://img.shields.io/badge/MRAID-3.0-purple)](https://www.iab.com/guidelines/mraid/)
[![Zero 3P Runtime Deps](https://img.shields.io/badge/Runtime%20Deps-Zero-brightgreen)](#zero-third-party-runtime-dependencies)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE)

---

## Demo App

<table>
  <tr>
    <td align="center">
      <img src="assets/demo_banner.gif" width="240" alt="Banner Ad"/><br/>
      <b>Banner + MRAID 3.0</b>
    </td>
    <td align="center">
      <img src="assets/demo_interstitial.gif" width="240" alt="Interstitial Ad"/><br/>
      <b>Interstitial</b>
    </td>
    <td align="center">
      <img src="assets/demo_native.gif" width="240" alt="Native Ad"/><br/>
      <b>IAB Native 1.2</b>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="assets/demo_appopen.gif" width="240" alt="App Open Ad"/><br/>
      <b>App Open Ads</b>
    </td>
    <td align="center" colspan="2">
      <img src="assets/demo_video.gif" width="240" alt="VAST 4.0 Rewarded Video"/><br/>
      <b>VAST 4.0 Rewarded Video</b>
    </td>
  </tr>
</table>

## What Sets This SDK Apart

Most ad SDKs (including major ones from Google, Meta, Unity) share a common set of problems. ApexAds was designed to solve all of them out of the box:

| Feature | ApexAds | AdMob | MAX | IronSource / LevelPlay |
|---|:---:|:---:|:---:|:---:|
| **Google Wallet Pass Ads** | ✅ | ❌ | ❌ | ❌ |
| App Open Ads (native SDK feature) | ✅ | ✅ | ❌ | ❌ |
| In-App / Header Bidding | ✅ | ❌ | ✅ (MAX) | ✅ (LevelPlay) |
| In-House Crash Reporting (no 3P SDK) | ✅ | ❌ | ❌ | ❌ |
| Custom Lightweight DI (no Hilt/Dagger) | ✅ | ❌ | ❌ | ❌ |
| Zero 3P Runtime Dependencies | ✅ | ❌ | ❌ | ❌ |
| AdMob Mediation Adapter | ✅ | — | ❌ | ❌ |
| MRC Viewability (frame-accurate) | ✅ | ✅ | ❌ | ❌ |
| MRAID 3.0 Rich Media | ✅ | ❌ | ❌ | ❌ |
| VAST 4.0 Rewarded Video | ✅ | ✅ | ✅ | ✅ |
| IAB TCF 2.0 Consent | ✅ | ✅ | ❌ | ❌ |
| IAB Native 1.2 | ✅ | ❌ | ❌ | ❌ |
| Optional feature modules (no transitive deps) | ✅ | ❌ | ❌ | ❌ |

### Differentiator Deep-Dives

#### 1. Google Wallet Pass Ads — Industry first

No other Android ad SDK supports Google Wallet pass delivery as an ad outcome. ApexAds embeds a native "Save to Google Wallet" CTA directly inside **existing** Interstitial and MRECT Banner formats — no separate ad format, no dedicated placement type, no DSP demand problem.

**The architecture insight:** A standalone `WalletAd` format would suffer near-zero fill rate because no DSP has demand for an opaque "wallet" placement. Instead, wallet is implemented as an optional *feature layer* on top of high-demand formats. Any interstitial or MRECT bid response can include an `ext.wallet` block; the SDK renders the CTA automatically when `sdk-wallet` is installed.

```kotlin
// Application.onCreate() — one line activates wallet CTAs across all eligible ads
ApexAds.init(this, config)
WalletAdExtension.install()  // registers WalletDelegate via ServiceLocator

// Interstitial — wallet CTA panel appears automatically if ext.wallet is in the bid
val interstitial = InterstitialAd.Builder("placement-interstitial")
    .listener(object : InterstitialAdListener {
        override fun onInterstitialLoaded()              { interstitial.show(activity) }
        override fun onInterstitialFailed(e: AdError)    { /* handle */ }
        override fun onWalletPassSaved()                 { grantLoyaltyPoints() }
        override fun onWalletPassCancelled()             { /* user dismissed */ }
        override fun onWalletPassFailed()                { /* Google Wallet unavailable */ }
    }).build()
interstitial.load()

// MRECT Banner — wallet CTA strip appears automatically at bottom of banner
val banner = BannerAd.Builder("placement-mrect")
    .adSize(AdSize.MRECT_300x250)
    .listener(object : BannerAdListener {
        override fun onAdLoaded()                        { banner.show(bannerAdView) }
        override fun onAdFailed(e: AdError)              { /* handle */ }
        override fun onWalletPassSaved()                 { showSuccessMessage() }
    }).build()
banner.load()
```

**How it works end-to-end:**

```
Publisher App                  SDK Core                      SDK Wallet (optional)
─────────────                  ────────                      ─────────────────────
WalletAdExtension.install() ──→ ServiceLocator.register
                                  (WalletDelegate)

InterstitialAd.load()       ──→ OpenRTBRequestBuilder
                                  detects WalletDelegate
                                  → imp.ext.wallet_supported=true
                               ──→ POST /openrtb2/auction
                               ←── BidResponse: ext.wallet { pass_jwt, cta_text, … }

InterstitialAd.show()       ──→ InterstitialActivity.onCreate
                                  adData.walletExtJson != null?
                                  → WalletDelegate.attachToInterstitial()
                                      builds bottom panel overlay
                                      "Save Coupon to Google Wallet" button

User taps CTA               ──→ WalletPassManager.savePassesJwt()
                               ←── Google Wallet result (onActivityResult)
                               ──→ WalletDelegate.handleActivityResult()
                               ──→ listener.onWalletPassSaved()
                               ──→ fire save_tracking_url (pixel)
```

**Zero dependency leakage:** `play-services-wallet` is scoped to `sdk-wallet` only. Publishers who don't call `WalletAdExtension.install()` never pull in Google Pay APIs and are unaffected.

#### 2. App Open Ads — Only the second SDK to offer this natively
Google AdMob introduced App Open as a dedicated ad format in 2021. No other Android ad SDK has followed. ApexAds implements the full lifecycle — background detection, frequency capping, automatic preload and re-preload after dismiss — backed by the same OpenRTB pipeline as every other format.

```kotlin
// Application.onCreate()
AppOpenAd.initialize(this, "placement-appopen", object : AppOpenAd.Listener {
    override fun onAppOpenAdLoaded()                    { /* ad is warm */ }
    override fun onAppOpenAdDismissed()                 { /* user dismissed */ }
    override fun onAppOpenAdFailedToLoad(error: AdError){ /* handle */ }
})
AppOpenAd.setFrequencyCapHours(1)   // show at most once per hour
AppOpenAd.setAdExpiryMinutes(30)    // discard stale cached ad after 30 min
```

#### 3. In-House Crash Reporting — Zero Sentry SDK dependency
Crash events are serialized to the Sentry envelope protocol and delivered via raw `HttpURLConnection` — no Sentry Android SDK on the classpath. The reporter installs a `Thread.UncaughtExceptionHandler`, retries delivery up to 3× with exponential back-off, and respects 429 rate limits.

```kotlin
ApexAdsConfig.Builder("APP_TOKEN")
    .sentryDsn("https://key@o123.ingest.sentry.io/project")
    .build()
// ↑ crash reporting is fully automatic after this. No other wiring needed.
```

#### 4. Custom DI — No Hilt, No Dagger, No Koin
Annotation processors from DI frameworks conflict with host app DI graphs and slow incremental builds. `ServiceLocator` is a `ConcurrentHashMap<Class, Any>` — 40 lines of code, zero reflection at runtime, zero transitive deps.

```kotlin
// SDK registers its own services at init time:
ServiceLocator.register(AdNetworkClient::class.java, HttpAdNetworkClient(config))
// Any module resolves them without knowing the concrete class:
val client = ServiceLocator.get(AdNetworkClient::class.java)
// Test code swaps in a mock with one line:
ServiceLocator.register(AdNetworkClient::class.java, MockAdExchange())
// Optional feature modules register their implementations via the same mechanism:
WalletAdExtension.install() // → ServiceLocator.register(WalletDelegate::class.java, …)
```

#### 5. Zero 3P Runtime Dependencies
Every SDK imported into a publisher app risks version conflicts with the publisher's own dependencies. ApexAds runtime uses only Android platform APIs:

| Replaced | With |
|---|---|
| OkHttp | `java.net.HttpURLConnection` |
| Gson | `org.json.JSONObject` (built into Android) |
| Timber | Custom `AdLog` over `android.util.Log` |
| Sentry SDK | Raw HTTP envelope protocol over `HttpURLConnection` |
| Hilt / Dagger | `ServiceLocator` (40-line `ConcurrentHashMap` wrapper) |

Optional feature modules (`sdk-wallet`) bring their own scoped dependencies without leaking them into `sdk-core` or the publisher's compile classpath.

---

## Architecture Overview

```mermaid
flowchart TD
    classDef app fill:#1565C0,color:#fff,stroke:#0D47A1
    classDef sdk fill:#2E7D32,color:#fff,stroke:#1B5E20
    classDef core fill:#4527A0,color:#fff,stroke:#311B92
    classDef ext fill:#E65100,color:#fff,stroke:#BF360C
    classDef wallet fill:#0277BD,color:#fff,stroke:#01579B

    subgraph DEMO["Demo App (MVVM)"]
        direction LR
        BF["BannerFragment"]:::app
        IF["InterstitialFragment"]:::app
        NF["NativeFragment"]:::app
        VF["VideoFragment"]:::app
        AOF["AppOpenFragment"]:::app
        IAB["InAppBiddingFragment"]:::app
        WF["WalletFragment"]:::app
        VM["AdViewModel"]:::app
    end

    subgraph MODULES["SDK Modules"]
        BAN["sdk-banner\n(MRAID 3.0)"]:::sdk
        INT["sdk-interstitial\n(Fullscreen)"]:::sdk
        NAT["sdk-native\n(IAB Native 1.2)"]:::sdk
        VID["sdk-video\n(VAST 4.0)"]:::sdk
        AOP["sdk-appopen\n(App Open)"]:::sdk
        IAB2["sdk-inappbidding\n(Header Bidding)"]:::sdk
        WAL["sdk-wallet\n(Google Wallet Pass)"]:::wallet
    end

    subgraph CORE["sdk-core"]
        ENTRY["ApexAds (init)"]:::core
        DI["ServiceLocator (DI)\n+ WalletDelegate interface"]:::core
        NET["HttpAdNetworkClient\n(HttpURLConnection)"]:::core
        PARSE["BidRequestSerializer\nBidResponseParser\n(org.json)"]:::core
        CACHE["AdCache (TTL)"]:::core
        CONSENT["ConsentManager\n(IAB TCF 2.0)"]:::core
        DEVICE["DeviceInfoProvider"]:::core
        IMPTRACK["ImpressionTracker\n(MRC)"]:::core
        CRASH["CrashReporter\n(Sentry envelope)"]:::core
        LOG["AdLog"]:::core
    end

    subgraph EXT["Ecosystem"]
        ADMOB["adapters-admob\n(AdMob Mediation)"]:::ext
        SENTRY["Sentry DSN endpoint"]:::ext
        EXCHANGE["Ad Exchange\n(OpenRTB 2.6)"]:::ext
        GWALLET["Google Wallet API"]:::wallet
    end

    DEMO --> MODULES
    MODULES --> CORE
    WAL --> GWALLET
    ADMOB --> MODULES
    NET --> EXCHANGE
    CRASH --> SENTRY
```

---

## Module Dependency Graph

```mermaid
graph LR
    classDef core fill:#4527A0,color:#fff,stroke:#311B92
    classDef module fill:#2E7D32,color:#fff,stroke:#1B5E20
    classDef wallet fill:#0277BD,color:#fff,stroke:#01579B
    classDef adapter fill:#E65100,color:#fff,stroke:#BF360C
    classDef app fill:#1565C0,color:#fff,stroke:#0D47A1

    CORE["sdk-core\n(WalletDelegate interface)"]:::core
    BAN["sdk-banner"]:::module
    INT["sdk-interstitial"]:::module
    NAT["sdk-native"]:::module
    VID["sdk-video"]:::module
    AOP["sdk-appopen"]:::module
    IAB["sdk-inappbidding"]:::module
    WAL["sdk-wallet\n(play-services-wallet)"]:::wallet
    ADM["adapters-admob"]:::adapter
    DEMO["demo-app"]:::app

    BAN --> CORE
    INT --> CORE
    NAT --> CORE
    VID --> CORE
    AOP --> INT
    IAB --> CORE
    WAL --> CORE
    ADM --> BAN
    ADM --> INT
    ADM --> VID
    DEMO --> BAN
    DEMO --> INT
    DEMO --> NAT
    DEMO --> VID
    DEMO --> AOP
    DEMO --> IAB
    DEMO --> WAL
```

| Module | Depends On | Responsibility |
|---|---|---|
| `sdk-core` | — (Android platform only) | SDK init, OpenRTB models, HTTP networking, ad cache, consent, DI, crash reporting, logging, `WalletDelegate` interface |
| `sdk-banner` | `sdk-core` | Banner ad + MRAID 3.0 WebView container; auto-attaches wallet CTA on MRECT when `WalletDelegate` is registered |
| `sdk-interstitial` | `sdk-core` | Fullscreen HTML interstitial; auto-attaches wallet bottom panel when `WalletDelegate` is registered |
| `sdk-native` | `sdk-core` | IAB Native 1.2 JSON parsing, publisher-controlled view binding |
| `sdk-video` | `sdk-core` | VAST 4.0 parsing, ExoPlayer rewarded video, quartile tracking |
| `sdk-appopen` | `sdk-interstitial` | App Open Ads — foreground detection, frequency cap, auto-preload |
| `sdk-inappbidding` | `sdk-core` | Header bidding price signals, MAX/LevelPlay mock simulation |
| `sdk-wallet` | `sdk-core` | Google Wallet pass delivery; `play-services-wallet` scoped here only — zero leakage |
| `adapters-admob` | `sdk-banner`, `sdk-interstitial`, `sdk-video` | AdMob mediation adapter (Banner, Interstitial, Rewarded) |
| `demo-app` | all modules | MVVM showcase app, MockAdExchange integration |

---

## Ad Request Lifecycle

```mermaid
sequenceDiagram
    participant App as Publisher App
    participant SDK as ApexAds SDK
    participant Cache as AdCache (TTL)
    participant Net as HttpAdNetworkClient
    participant Exch as Ad Exchange (OpenRTB)

    App->>SDK: ad.load()
    SDK->>Cache: get(format, placementId)
    alt Cache hit (not expired)
        Cache-->>SDK: AdData
        SDK-->>App: onAdLoaded()
    else Cache miss
        SDK->>Net: requestBid(BidRequest)
        Note over Net,Exch: imp.ext.wallet_supported=true\n(if WalletAdExtension installed + MRECT/Interstitial)
        Net->>Exch: POST /openrtb2/auction
        Exch-->>Net: BidResponse (JSON)
        Note over Net,SDK: ext.wallet parsed → AdData.walletExtJson
        Net-->>SDK: BidResponse
        SDK->>Cache: put(format, placementId, AdData)
        SDK-->>App: onAdLoaded()
    end
    App->>SDK: ad.show(activity)
    SDK->>App: Render ad (Activity / WebView / ExoPlayer)
    opt AdData carries ext.wallet and WalletDelegate registered
        SDK->>App: Overlay "Save to Google Wallet" CTA
        App->>SDK: User taps CTA
        SDK->>SDK: WalletPassManager.savePassesJwt()
        SDK-->>App: onWalletPassSaved / Cancelled / Failed
        SDK->>Net: fireTrackingUrl(save_tracking_url)
    end
    SDK->>Net: fireTrackingUrl(winNoticeUrl)
    Net->>Exch: GET win notice (fire-and-forget)
```

---

## App Open Ad Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Preloading : AppOpenAd.initialize()
    Preloading --> Ready : onInterstitialLoaded
    Preloading --> Idle : load failed
    Idle --> Preloading : retry (next launch)
    Ready --> Checking : App foregrounds
    Checking --> Showing : Frequency cap satisfied\n+ ad not expired
    Checking --> Ready : Cap not satisfied\nor ad expired
    Showing --> Preloading : Ad dismissed
    Showing --> [*] : AppOpenAd.destroy()
```

---

## Ad Tech Standards Implemented

### OpenRTB 2.6 (IAB)
Full bid request/response object graph — `BidRequest`, `Impression`, `Banner`, `Video`, `Native`, `App`, `Device`, `User`, `Regs`, `Geo`, and all extension objects. Serialized to JSON with hand-written `org.json` serializer (no Gson/Moshi). Supports `imp.ext` for capability signalling (e.g. `wallet_supported: true`).

```kotlin
val request = OpenRTBRequestBuilder(deviceInfoProvider, consentManager)
    .adFormat(AdFormat.BANNER)
    .adSize(AdSize.MRECT_300x250)
    .placementId("placement-mrect")
    .bidFloor(0.50)
    .build()
// → POSTs JSON to ad exchange endpoint, parses BidResponse
// → automatically includes imp.ext.wallet_supported=true when sdk-wallet is installed
```

### MRAID 3.0 (IAB)
Full JavaScript bridge injected into ad WebViews. Implements `mraid.close()`, `mraid.expand()`, `mraid.resize()`, `mraid.open()`, `mraid.getState()`, `mraid.isViewable()`, `mraid.supports()`, and all lifecycle events.

### VAST 4.0 (IAB)
Pure-Java XML parser handles Inline and Wrapper VAST. Quartile tracking events (start, 25%, 50%, 75%, complete) fire automatically via ExoPlayer position polling every 500ms.

### IAB TCF 2.0 / GDPR
Reads `IABTCF_TCString`, `IABTCF_gdprApplies`, and `IABUSPrivacy_String` from SharedPreferences — the standardised keys written by any compliant CMP. Consent signals are auto-wired into `Regs` and `User` OpenRTB objects.

### MRC Viewability Standard
`ImpressionTracker` attaches a `ViewTreeObserver.OnPreDrawListener` to measure visible pixel ratio every frame. Impression fires only when ≥50% of pixels are in-view for ≥1 continuous second. Properly cleans up the listener on view detach (memory-safe).

### In-App Bidding / Header Bidding
`ApexInAppBidder` fetches a real-time bid from the exchange and packages it as a `BidToken` price signal — compatible with MAX and LevelPlay's `setLocalExtraParameter` / `setSignal` patterns.

### Google Wallet Pass Ads (ext.wallet)
Non-standard OpenRTB extension (`ext.wallet`) carries a signed Google Wallet pass JWT alongside the creative. The SDK parses this, presents a native "Save to Google Wallet" CTA, and invokes `PayClient.savePassesJwt()` — with result handling, tracking pixel, and publisher callbacks.

---

## Project Structure

```
apex-ad-sdk-android/
│
├── sdk-core/                          # Zero 3P runtime deps — Android platform APIs only
│   └── com/apexads/sdk/
│       ├── ApexAds.java               # SDK singleton entry point
│       ├── ApexAdsConfig.java         # Immutable builder-pattern configuration
│       └── core/
│           ├── di/
│           │   ├── ServiceLocator     # Lightweight ConcurrentHashMap DI (40 lines)
│           │   └── WalletDelegate     # Interface — decouples sdk-core from sdk-wallet
│           ├── network/
│           │   ├── AdNetworkClient    # Interface (2 methods)
│           │   ├── HttpAdNetworkClient# HttpURLConnection impl (no OkHttp)
│           │   ├── SdkHttpClient      # Raw HTTP helper
│           │   ├── BidRequestSerializer  # org.json serializer (no Gson)
│           │   └── BidResponseParser     # org.json parser + ext.wallet extraction
│           ├── models/openrtb/        # Full OpenRTB 2.6 POJO graph
│           ├── request/OpenRTBRequestBuilder  # Auto-signals wallet_supported
│           ├── cache/AdCache          # Thread-safe TTL-based ad cache
│           ├── consent/ConsentManager # IAB TCF 2.0 SharedPreferences reader
│           ├── device/DeviceInfoProvider
│           ├── tracking/ImpressionTracker  # MRC viewability (frame-accurate)
│           ├── crashreporter/         # In-house Sentry envelope reporter
│           │   ├── SentryDsn          # DSN parser → envelope URL
│           │   ├── CrashEvent         # Sentry envelope serializer (no Sentry SDK)
│           │   ├── CrashDelivery      # HTTP POST, 3× retry, 429-aware
│           │   └── CrashReporter      # UncaughtExceptionHandler installer
│           ├── error/AdError          # Typed error sealed hierarchy
│           └── utils/AdLog            # android.util.Log wrapper (no Timber)
│
├── sdk-banner/                        # Banner + MRAID 3.0
│   └── BannerAd / BannerAdView        # Auto-attaches wallet CTA strip on MRECT
│   └── mraid/MRAIDBridge
│
├── sdk-interstitial/                  # Fullscreen interstitial
│   └── InterstitialAd / InterstitialActivity  # Auto-attaches wallet bottom panel
│
├── sdk-native/                        # IAB Native 1.2
│   └── NativeAd / NativeAdParser (org.json) / NativeAdView
│
├── sdk-video/                         # VAST 4.0 rewarded video
│   └── VideoAd / VideoAdActivity / vast/VastParser
│
├── sdk-appopen/                       # App Open Ads  ◀ unique to ApexAds + AdMob
│   └── AppOpenAd              # Public static facade
│   └── AppOpenAdManager       # Singleton lifecycle manager
│   └── AppOpenAdFrequencyCap  # SharedPreferences frequency cap
│
├── sdk-wallet/                        # Google Wallet Pass Ads  ◀ unique to ApexAds
│   └── WalletAdExtension      # install() — one call enables wallet across all formats
│   └── WalletDelegateImpl     # Implements WalletDelegate; builds CTA panels
│   └── WalletResultActivity   # Transparent Activity for banner wallet flow
│   └── WalletPassData         # Parsed ext.wallet value object
│   └── WalletPassManager      # Thin PayClient wrapper (stateless)
│
├── sdk-inappbidding/                  # Header bidding / in-app bidding
│   └── ApexInAppBidder / BidToken / InAppBidListener
│   └── mock/MockMediationPlatform     # Simulates MAX/LevelPlay waterfall
│
├── adapters-admob/                    # AdMob mediation adapters
│   └── ApexAdsAdMobAdapter    # Main entry point (extends Adapter)
│   └── ApexAdsBannerAdapter / ApexAdsInterstitialAdapter / ApexAdsRewardedAdapter
│
└── demo-app/                          # MVVM showcase (no live server needed)
    └── DemoApplication        # SDK init + MockAdExchange + AppOpenAd + WalletAdExtension
    └── ui/banner / interstitial / native / video / appopen / inappbidding / wallet
```

---

## Integration Guide

### 1. Initialize the SDK

```kotlin
// Application.onCreate()
ApexAds.init(this, ApexAdsConfig.Builder("YOUR_APP_TOKEN")
    .adServerUrl("https://your-openrtb-endpoint.com/auction")
    .cacheTtlSeconds(300)
    .gdprConsentString(tcfString)
    .sentryDsn("https://key@o123.ingest.sentry.io/project") // optional crash reporting
    .build())
```

### 2. Google Wallet Pass Ads

Add the `sdk-wallet` module to activate wallet CTAs inside Interstitial and MRECT Banner ads:

```kotlin
// Application.onCreate() — after ApexAds.init()
WalletAdExtension.install()
// That's it. No changes to existing InterstitialAd or BannerAd code required.
// The SDK signals wallet_supported=true to the exchange and renders the CTA automatically.
```

```kotlin
// Existing interstitial — wallet CTA panel appears automatically when ext.wallet is present
val interstitial = InterstitialAd.Builder("placement-interstitial")
    .listener(object : InterstitialAdListener {
        override fun onInterstitialLoaded()            { interstitial.show(activity) }
        override fun onInterstitialFailed(e: AdError)  { /* handle */ }
        // New wallet callbacks — default no-op if not overridden
        override fun onWalletPassSaved()               { grantLoyaltyPoints() }
        override fun onWalletPassCancelled()           { /* user dismissed */ }
        override fun onWalletPassFailed()              { /* Google Wallet unavailable */ }
    }).build()
interstitial.load()

// Existing MRECT banner — wallet CTA strip appears automatically when ext.wallet is present
val banner = BannerAd.Builder("placement-mrect")
    .adSize(AdSize.MRECT_300x250)
    .listener(object : BannerAdListener {
        override fun onAdLoaded()                      { banner.show(bannerAdView) }
        override fun onAdFailed(e: AdError)            { /* handle */ }
        override fun onWalletPassSaved()               { showSuccessMessage() }
    }).build()
banner.load()
```

> **Note:** `play-services-wallet` is a dependency of `sdk-wallet` only. Publishers who omit `sdk-wallet` from their Gradle build — or who never call `WalletAdExtension.install()` — are completely unaffected. Ads load and display normally; the wallet CTA is simply absent.

### 3. App Open Ads

```kotlin
// Application.onCreate() — fires automatically on every background→foreground
AppOpenAd.initialize(this, "placement-appopen", listener)
AppOpenAd.setFrequencyCapHours(1)
AppOpenAd.setAdExpiryMinutes(30)
```

### 4. Banner

```xml
<com.apexads.sdk.banner.BannerAdView
    android:id="@+id/banner_ad_view"
    android:layout_width="320dp"
    android:layout_height="50dp" />
```

```kotlin
val banner = BannerAd.Builder("placement-banner")
    .adSize(AdSize.BANNER_320x50)
    .listener(object : BannerAdListener {
        override fun onAdLoaded() = banner.show(bannerAdView)
        override fun onAdFailed(error: AdError) = handleError(error)
    }).build()
banner.load()
```

### 5. Interstitial

```kotlin
val interstitial = InterstitialAd.Builder("placement-interstitial")
    .listener(object : InterstitialAdListener {
        override fun onInterstitialLoaded() { /* show at natural pause */ }
        override fun onInterstitialFailed(error: AdError) = handleError(error)
    }).build()
interstitial.load()
// at a content transition:
if (interstitial.isReady()) interstitial.show(activity)
```

### 6. Rewarded Video

```kotlin
val video = VideoAd.Builder("placement-video")
    .listener(object : VideoAdListener {
        override fun onVideoAdLoaded()  { showButton.isEnabled = true }
        override fun onRewardEarned()   { grantReward() }
        override fun onVideoAdFailed(error: AdError) = handleError(error)
    }).build()
video.load()
```

### 7. Native

```kotlin
val native = NativeAd.Builder("placement-native")
    .listener(object : NativeAdListener {
        override fun onNativeAdLoaded(ad: NativeAd) = ad.bindTo(nativeAdView)
        override fun onNativeAdFailed(error: AdError) = handleError(error)
    }).build()
native.load()
```

### 8. In-App Bidding (Header Bidding)

```kotlin
// Call before loading your mediation ad:
ApexInAppBidder.fetchBidToken("placement-001", AdFormat.INTERSTITIAL, object : InAppBidListener {
    override fun onBidReady(token: BidToken) {
        // Pass price signal to MAX / LevelPlay:
        maxAd.setLocalExtraParameter("apex_bid_token", token.token)
        maxAd.setLocalExtraParameter("apex_bid_cpm", token.cpmUsd.toString())
        maxAd.loadAd()
    }
    override fun onBidFailed(error: AdError) = maxAd.loadAd() // waterfall fallback
})
```

### 9. AdMob Mediation

Configure in the AdMob dashboard:
- **Class name:** `com.apexads.sdk.adapters.admob.ApexAdsAdMobAdapter`
- **Server parameters:** `{"placementId":"your-id","appToken":"your-token"}`

No additional code needed — AdMob calls the adapter automatically.

---

## Demo App

The demo app uses `MockAdExchange` — an in-process OpenRTB mock that returns realistic bid responses. No backend setup, API key, or network required.

| Tab | Format | Demonstrates |
|---|---|---|
| Banner | 320×50 HTML + MRAID | OpenRTB auction → MRAID 3.0 WebView → MRC viewability |
| Interstitial | Fullscreen HTML | Pre-load / show lifecycle, MRAID close, separate Activity |
| Native | IAB Native 1.2 | OpenRTB native request, org.json asset parsing, publisher layout |
| Video | VAST 4.0 Rewarded | VAST parsing, ExoPlayer, quartile tracking, skip button, reward |
| App Open | Fullscreen Interstitial | Background→foreground detection, frequency cap, auto-preload |
| In-App Bidding | Price signal | Header bidding token → mock MAX/LevelPlay waterfall auction |
| **Wallet** | **Interstitial + MRECT** | **Google Wallet CTA in existing formats; ext.wallet from MockAdExchange** |

```bash
git clone https://github.com/Madroid2/apex-ad-sdk-android.git
cd apex-ad-sdk-android
./gradlew :demo-app:installDebug
```

---

## Standards & References

| Standard | Version | Reference |
|---|---|---|
| OpenRTB | 2.6 | [IAB OpenRTB 2.6](https://www.iab.com/wp-content/uploads/2022/04/OpenRTB-2-6_FINAL.pdf) |
| VAST | 4.0 | [IAB VAST 4.0](https://www.iab.com/guidelines/vast/) |
| MRAID | 3.0 | [IAB MRAID 3.0](https://www.iab.com/guidelines/mraid/) |
| OpenRTB Native | 1.2 | [IAB Native Ads API](https://www.iab.com/guidelines/openrtb-native-ads-api-specification-version-1-2/) |
| TCF | 2.0 | [IAB TCF 2.0](https://github.com/InteractiveAdvertisingBureau/GDPR-Transparency-and-Consent-Framework) |
| MRC Viewability | — | [MRC Display Measurement Guidelines](https://www.mediaratingcouncil.org) |
| Sentry Envelope | — | [Sentry Envelope Protocol](https://develop.sentry.dev/sdk/data-model/envelopes/) |
| Google Wallet | — | [Google Wallet Passes API](https://developers.google.com/wallet/generic/android) |
