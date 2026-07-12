---
name: apex-sdk-handover
description: Principal-engineer handover for the Apex Android Ad SDK. Read this before doing any work in apex-ad-sdk-android — architecture, invariants, build/test commands, cross-repo contracts with apex-ad-server and apex-demand-platform, known gotchas, and the production roadmap. Use when planning features, fixing bugs, reviewing PRs, or answering questions about the SDK.
---

# Apex Ad SDK (Android) — Principal Engineer Handover

This is the knowledge-transfer document for the retiring principal engineer.
It captures what the code cannot tell you: why things are the way they are,
what must never be broken, and where the project is heading.

## 1. The Apex Ecosystem (three repos, one product)

| Repo | Role | Stack |
|---|---|---|
| `apex-ad-sdk-android` (this repo) | Publisher-side ad SDK: renders ads, sends OpenRTB 2.6 bid requests | Java (sdk-core is Java-only), Kotlin where needed, Gradle KTS, min API 21, JDK 17 |
| `apex-ad-server` | SSP / first-price auction engine, consent enforcement, demand fan-out | Go 1.23, only dep is `gopkg.in/yaml.v3` |
| `apex-demand-platform` | Self-serve DSP: advertiser portal, AI creative generation + review agent, bidder | Next.js 16 / React 19 / TypeScript, `node:sqlite` |

**Request flow:** SDK → `POST /openrtb/v1/auction` on apex-ad-server (auth:
`X-ApexAds-Token` header) → server fans out in parallel to House DSP (in-process),
MockDSP, Prebid Server, and apex-demand-platform (`POST /api/dsp/bid`) → first-price
winner returned to the SDK → SDK renders and fires notice URLs.

### Cross-repo contracts (breaking any of these breaks another repo)

- **OpenRTB 2.6 JSON** is the wire format everywhere. Field names in
  `sdk-core/models/openrtb/` must match apex-ad-server's `internal/model/openrtb.go`
  and the demand platform's `lib/schemas.ts`.
- **`imp.ext.wallet_supported: true`** — SDK signals wallet capability when
  `WalletAdExtension.install()` was called. The demand platform only attaches
  `bid.ext.wallet` when it sees this flag.
- **`bid.ext.wallet`** — `{ pass_jwt, cta_text, save_tracking_url, … }`. The SDK
  parses this into `AdData.walletExtJson` and fires `save_tracking_url` after
  Google Wallet confirms the save.
- **`user.data[].segment[]`** — first-party audience cohorts. The SDK attaches them
  only with TCF Purpose 4 consent; the ad server strips them on every
  non-personalised path; the demand platform assumes they arrive pre-gated.
- **`nbr = 2`** is the SDK-wide convention for OpenRTB no-bid/no-fill.
- **In-app bidding token flow:** `ApexInAppBidder` → `POST /inapp/v1/signal`
  (server runs full auction, caches creative 5 min, returns opaque token + CPM) →
  mediation platform → `POST /inapp/v1/bid` with token in `user.buyeruid` →
  cached creative returned.
- **Notice semantics:** `nurl` = win (fires on load), `burl` = billing (fires at
  the MRC-viewable impression in `ImpressionTracker`), `lurl` = loss (reason 102,
  fired by `OpenRTBAdRepository` for losing bids). `AuctionMacros` expands
  `${AUCTION_PRICE}` / `${AUCTION_ID}` in `AdData.fromBid` — a notice fired with
  the literal macro is a dropped event on the bidder side.
- **Test-mode bypass:** `ext.apex.testmode=1` skips consent enforcement on the
  server — debug builds only.

## 2. Non-negotiable invariants

1. **Zero third-party runtime dependencies in `sdk-core`.** It is Java-only and
   uses Android platform APIs exclusively: `org.json` (not Gson), `HttpURLConnection`
   (not OkHttp), `android.util.Log` via `AdLog` (not Timber), raw Sentry envelope
   protocol over HTTP (not the Sentry SDK), and a hand-rolled DI
   (`ApexServices` + `FeatureRegistry`/`ServiceLocator` — no Hilt/Dagger/Koin,
   zero reflection). This is the SDK's core commercial differentiator; do not
   add a dependency to sdk-core, ever.
2. **Optional deps stay scoped to feature modules.** `play-services-wallet` lives
   only in `sdk-wallet`. A publisher who doesn't install a feature module must
   not receive its APIs or dependencies transitively.
3. **Honest demand only.** If every demand source no-fills, return an honest
   no-fill (`nbr=2`). Mock or house creatives must never be presented as paid
   demand. `debugFakeFill` appends `MockAdExchange` as lowest-priority demand in
   debug builds only; non-debug builds ignore the flag. `FallbackAdNetworkClient`
   is deprecated — it no longer substitutes mock demand.
4. **Never fabricate device signals.** The user agent is the real WebView UA
   (`WebSettings.getDefaultUserAgent`, cached). A hardcoded UA is an IVT flag at
   exchanges. A zeroed GAID (`00000000-…`) means opt-out: normalize to `null` +
   `limitAdTracking=true`; never send it as a live `ifa`.
5. **OMID signaling is honesty-gated.** `api=7` and `source.ext.omidpn/omidpv`
   are added only while `MeasurementDelegate.isReady()` is true. The scaffold
   reports not-ready until the real IAB OM SDK AAR is bundled. Never signal OMID
   without live measurement.
6. **Tracking failures never surface to the publisher app.** Impression, click,
   win, and wallet-save tracking errors may be logged internally but must not
   crash or block rendering.
7. **Public package names are frozen.** Source files are organized by MVVM layer
   folders (`presentation/`, `domain/`, `data/`, `di/`) but the Java package
   declarations keep the original publisher-facing names
   (e.g. `com.apexads.sdk.banner.BannerAdView`). Never rename a public package
   to match a folder path — it breaks binary/source compatibility of the
   published Maven artifacts.
8. **`ApexAds` stays tiny.** `init(...)` is the only public singleton API.
   Runtime service access, test resets, network overrides, and cohort-rule
   updates live behind library-internal `ApexSdkRuntime`. ViewModels depend on
   `AdRepository` + domain models only — never call runtime singletons directly.

## 3. Architecture in one page

- **Custom MVVM, no AndroidX ViewModel** (third-party SDKs have no
  `ViewModelStoreOwner`; pattern mirrors Smaato ng-sdk-android). Base layer in
  `sdk-core/.../mvvm/`: abstract `AdViewModel` (load/show/destroy lifecycle),
  `AdStateObservable` (weak-ref observers so views never leak activities),
  `AdRepository` interface with `OpenRTBAdRepository` impl (IO-thread dispatch,
  main-thread callbacks), `AdState` enum:
  `IDLE → LOADING → LOADED → DISPLAYED → EXPIRED/FAILED` (EXPIRED/FAILED → LOADING on retry).
- **`onAdLoaded() throws AdError` hook:** format subclasses (VAST XML parsing in
  `VideoAdViewModel`, IAB Native 1.2 JSON in `NativeAdViewModel`) signal parse
  failure by throwing; the base class catches and transitions to FAILED. No
  re-entrant callback chains.
- **Facades are thin.** `BannerAd`, `InterstitialAd`, `VideoAd`, `NativeAd` own a
  ViewModel and bridge format listeners to `AdViewModelListener`. Zero business
  logic in facades.
- **Module graph:** all format modules depend only on `sdk-core`;
  `sdk-appopen` → `sdk-interstitial`; `adapters-admob` → banner/interstitial/video;
  `sdk-wallet` and `sdk-measurement` are optional feature modules registering
  `SdkFeature` contracts (`WalletDelegate`, `MeasurementDelegate`) via
  `ServiceLocator`.
- **Modules (Maven artifacts, group `com.apexads`):** sdk-core, sdk-banner
  (MRAID 3.0), sdk-interstitial, sdk-native (IAB Native 1.2 — Gradle namespace is
  `nativeads` because `native` is a Java keyword), sdk-video (VAST 4.0 +
  ExoPlayer + quartile tracking), sdk-appopen, sdk-inappbidding, sdk-wallet,
  sdk-measurement (OMID scaffold), adapters-admob, demo-app.
- **Standards implemented:** OpenRTB 2.6, VAST 4.0 (inline + wrapper), MRAID 3.0,
  IAB Native 1.2, IAB TCF 2.0 (SharedPreferences keys `IABTCF_TCString`,
  `IABTCF_gdprApplies`, `IABUSPrivacy_String`, `IABTCF_PurposeConsents`),
  MRC viewability (≥50% pixels for ≥1s via `ViewTreeObserver.OnPreDrawListener`),
  single-node SupplyChain (`source.schain` + `source.ext.schain` mirror).

## 4. Subsystem gotchas (hard-won knowledge)

- **GAID without Play Services:** `AdvertisingIdProvider` binds the GMS
  advertising-ID service and speaks its AIDL wire protocol over a raw `IBinder`
  (Amazon Fire OS `Settings.Secure` fallback). Identity resolution is IPC and
  must never block a bid request — the provider caches a volatile snapshot;
  `DeviceInfoProvider.warmUp()` refreshes it on the IO executor at init.
  `ifa`/`user.id` are gated on: no LAT, COPPA off, and (under GDPR) TCF Purpose 1
  via `ConsentManager.canShareDeviceIdentifiers()`.
- **Wallet (MRECT banner path):** the SDK cannot intercept the publisher
  activity result, so MRECT wallet CTAs route through `WalletResultActivity`, a
  short-lived transparent proxy that uses **static handoff slots** for the pass
  JWT, tracking URL, and callback — these must be cleared in `onDestroy`.
  Interstitial wallet CTAs run inside the publisher activity via the active
  wallet session. Wallet sessions hold view references weakly. In SDK test mode
  the mock JWT is not sent to Google Wallet; success is simulated.
- **App Open Ads:** background→foreground detection uses
  `Application.ActivityLifecycleCallbacks`; the app counts as backgrounded only
  when the started-activity count reaches zero, and **cold start is not a
  foreground return**. Manager intentionally holds the application context;
  activity refs are weak. Frequency cap (SharedPreferences) and cached-ad
  expiry are checked before show.
- **Build config:** release builds point at the production ad-server URL in
  `sdk-core/build.gradle.kts`; debug builds use `10.0.2.2` (emulator → host).
  `androidx.annotation` is force-resolved to `1.8.1` across all subprojects
  (consistent-resolution conflicts otherwise). ProGuard consumer rules keep
  public entry points and OpenRTB model fields unobfuscated (org.json
  serialization reads the fields).
- **MockAdExchange:** in-process dev exchange emitting banner, interstitial,
  MRECT-wallet, VAST 4.0, and Native 1.2 samples. Tracking URLs are deliberately
  non-operational. VAST sample media is a public Big Buck Bunny MP4 — if it dies,
  swap in another small stable public MP4 and update tests.
- **Comment policy (docs/SDK_NOTES.md):** keep code comments only when they
  prevent a real maintenance mistake; design rationale goes in `docs/`, not
  source comments.

## 5. Build, test, CI, publish

```bash
./gradlew testDebugUnitTest jacocoDebugUnitTestReport jacocoDebugCoverageVerification
./gradlew :demo-app:assembleDebugAndroidTest   # instrumentation tests COMPILE only in CI
./gradlew :demo-app:installDebug               # demo app; works offline via MockAdExchange
```

- CI: `.github/workflows/android-tests.yml`, JDK 17, ubuntu-latest.
  **80% JaCoCo line-coverage gate** with `businessLogicExcludes` in the root
  `build.gradle.kts` excluding views/activities/adapters/UI from the denominator.
- On green `main` pushes CI publishes all artifacts to **GitHub Packages**
  (`maven.pkg.github.com/Madroid2/apex-ad-sdk-android`) as
  `1.0.<run-number>-SNAPSHOT`. Consumers need a PAT with `read:packages` — this
  friction is a known gap; Maven Central publishing is Phase 0 roadmap.
- Test layout mirrors production layers (parser tests under `data/parser`,
  ViewModel tests under `presentation/…`, etc.).

## 6. Where the project is going (docs/PRODUCTION_ROADMAP.md is the source of truth)

The SDK does the *ad-serving* half well; the *operational trust layer* is the gap.
P0 items (deal-breakers for a DSP partner like Smadex): OM SDK integration +
certification, TCF 2.2 + IAB GPP + consent *enforcement* matrix, fleet telemetry +
remote kill switch, persisted/batched event queue (lost billing events are lost
revenue), R8-minified release AARs, Maven Central, API-stability gate (BCV).
Recent commits landed the trust-layer basics: real GAID, real UA, schain, macros,
burl/lurl semantics, OMID scaffolding.

**Mediation architecture (docs/MEDIATION_ARCHITECTURE.md):** two topologies.
A = Apex as a demand source inside AdMob (exists: `adapters-admob` implements
Google's custom-event API). B = Apex as the mediator (planned:
`MediationController` + `ApexMediationAdapter` SPI, hybrid parallel-bidding +
price-ordered waterfall + house backfill). You cannot legitimately chain rival
mediators (AdMob → MAX → ironSource); the fallback set is: Apex exchange →
bidding networks → house/cross-promo. `MockMediationPlatform` in
sdk-inappbidding is demo-only.

## 7. First moves for a new maintainer

1. Read `docs/SDK_NOTES.md` (ADR-style rationale), then `docs/PRODUCTION_ROADMAP.md`.
2. Run the demo app — it exercises every format offline through MockAdExchange.
3. For full-stack work, run apex-ad-server (`docker compose up --build`, port
   8080) and apex-demand-platform (`npm run dev`, port 3000) and point a debug
   build at `10.0.2.2:8080`.
4. Before touching bid-request code, diff against the OpenRTB 2.6 spec and the
   server's model — field-name drift is the most common cross-repo bug.
