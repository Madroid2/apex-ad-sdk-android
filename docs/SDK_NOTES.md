# Apex SDK Notes

These notes hold design rationale that previously lived in source comments. Keep
implementation files terse; add ADR-style context here or in a more specific doc.

## Comment Policy

- Keep code comments only when they prevent a real maintenance mistake.
- Prefer clear names, small methods, and tests over comments that restate code.
- Move setup examples, architecture rationale, module boundaries, and historical
  decisions into docs.
- Keep public API comments short when they describe callback timing or external
  contracts that are not obvious from the signature.

## Runtime Dependency Boundaries

- `sdk-core` is Java-only and avoids third-party runtime dependencies. It relies
  on Android platform APIs such as `org.json`, `HttpURLConnection`, and
  `android.util.Log`.
- SDK modules intentionally avoid the Kotlin Gradle plugin unless a module adds
  Kotlin production sources.
- The `sdk-native` Gradle namespace uses `nativeads` because `native` is a Java
  keyword.
- Optional feature dependencies should stay scoped to their feature module.
  `play-services-wallet` belongs in `sdk-wallet`; apps that do not install
  wallet support should not receive wallet APIs transitively.
- `ApexServices` owns typed core services. Feature lookup/registration stays
  behind library-internal `ApexFeatureAccess`/`ServiceLocator` helpers, not on
  the publisher-facing `ApexAds` singleton.
- `ApexAds` is intentionally tiny: `init(...)` is the only public singleton API.
  Runtime service access, test resets, network overrides, and cohort-rule
  updates stay behind library-internal `ApexSdkRuntime`.
- `ApexSdkRuntime` is the library-internal composition root. Public ad facades
  may use it to assemble ViewModels and repositories, but ViewModels should not
  call runtime singletons directly.
- ViewModels depend on `AdRepository` and SDK/domain models. Concrete OpenRTB,
  cache TTL, device, consent, and network details stay in the data/runtime layer.

## Build Configuration

- Release builds point at the production Apex Ad Server URL declared in
  `sdk-core/build.gradle.kts`.
- Debug builds use `10.0.2.2` so Android emulators can reach an ad server running
  on the host machine.
- Core ProGuard rules are embedded through `consumerProguardFiles` when the AAR
  is consumed by an app.
- Keep public SDK entry points and OpenRTB model fields from being obfuscated or
  stripped because publishers call those APIs directly and JSON serialization
  relies on the model fields.

## Demand and No-Fill Rules

- `ApexAds.init()` delegates to `ApexSdkRuntime`, which registers real OpenRTB demand first through
  `HttpAdNetworkClient`.
- `WaterfallAdNetworkClient` tries demand sources in priority order and returns
  the first genuine priced creative.
- If every source fails or no-fills, the SDK returns an honest OpenRTB no-fill.
  Mock or house creatives must not be presented as paid demand.
- `debugFakeFill` is for local development and CI only. It appends
  `MockAdExchange` as a lowest-priority source in debug builds so the SDK can
  render without a live server. Non-debug builds ignore this flag.
- `FallbackAdNetworkClient` is deprecated. It no longer substitutes mock demand;
  it delegates to the live primary client and preserves no-fill responses.
- `nbr = 2` is the SDK convention for OpenRTB no-bid/no-fill responses.

## Mock Exchange

- `MockAdExchange` is an in-process development exchange.
- It emits representative banner, interstitial, MRECT wallet, VAST 4.0, and IAB
  Native 1.2 responses.
- Tracking URLs in mock responses are sample URLs. Mock tracking is intentionally
  non-operational.
- The VAST sample media is a stable public Big Buck Bunny MP4 used for parser and
  render testing. If that media URL stops working, replace it with another small
  stable public MP4 and update tests if needed.

## Tracking

- Tracking pixel failures must never surface to the publisher app.
- Tracking transport may log failures internally, but impression, click, win, and
  wallet-save tracking errors should not crash or block rendering.

## App Open Ads

- App Open Ads use `Application.ActivityLifecycleCallbacks` to detect a real
  background-to-foreground transition.
- Cold start is not treated as a foreground return. The app is marked backgrounded
  only after the started activity count reaches zero.
- The manager keeps the application context intentionally. Activity references
  are weak and cleared when the app backgrounds.
- Frequency caps and cached-ad expiry are enforced before showing.

## Wallet Feature

- Wallet support is activated by `WalletAdExtension.install()`, which registers
  the wallet delegate as an optional `SdkFeature`.
- Interstitial wallet CTAs are handled inside the publisher activity, so result
  delivery is routed through the active wallet session.
- MRECT banner wallet CTAs use `WalletResultActivity` as a short-lived proxy
  because the SDK cannot intercept the publisher activity result directly.
- `WalletResultActivity` uses static handoff slots for the pass JWT, tracking
  URL, and callback. They must be cleared in `onDestroy`.
- Wallet sessions store view references weakly so an activity destroy does not
  leak views while waiting for the Google Wallet result.
- In SDK test mode, the mock JWT is not sent to Google Wallet. The SDK simulates
  success so the wallet CTA UX can be demoed without a live issuer account.

## In-App Bidding

- `ApexInAppBidder` requests an opaque bid token from `/inapp/v1/signal`.
- Mediation integrations should pass the token and CPM to the chosen mediation
  platform using the platform's supported local-extra or bidding API.
- `MockMediationPlatform` is demo-only. It models coarse CPM floors so the demo
  can show Apex winning or falling back without bundling MAX or LevelPlay SDKs.

## OpenRTB Serialization

- `BidRequestSerializer` and `BidResponseParser` use Android `org.json`.
- Keep OpenRTB field names aligned with the OpenRTB 2.6 model classes.
- Request `ext` may include caller-provided fields plus Apex-specific extension
  values under the same `ext` object.

## Device Identity (Trust Layer)

- `AdvertisingIdProvider` resolves GAID without any Play Services library: it
  binds the GMS advertising-ID service and speaks its AIDL wire protocol over a
  raw `IBinder`, with an Amazon Fire OS `Settings.Secure` fallback. This keeps
  the zero-runtime-dependency guarantee.
- A zeroed GAID (`00000000-…`) means the user opted out. It is normalized to
  `null` + `limitAdTracking=true` and must never be sent as a live `ifa`.
- Identity resolution is IPC and must never block a bid request. The provider
  caches a volatile snapshot; `DeviceInfoProvider.warmUp()` refreshes it on the
  IO executor at SDK init.
- The bid-request `ifa`/`user.id` are gated on: no LAT, COPPA off, and (under
  GDPR) TCF Purpose 1 via `ConsentManager.canShareDeviceIdentifiers()`.
- The device user agent is the real WebView UA
  (`WebSettings.getDefaultUserAgent`), cached after first resolution. A
  fabricated UA that mismatches the device is an IVT flag at exchanges — do not
  reintroduce a hardcoded UA string.

## Supply Chain & Event Notices (Trust Layer)

- Every bid request carries a complete single-node SupplyChain object
  (`source.schain` for OpenRTB 2.6 plus a `source.ext.schain` mirror for
  2.5-era exchanges). Node `asi`/`sid` come from `ApexAdsConfig`
  (`supplyChainDomain`, `sellerId`, defaulting to the app token).
- `AuctionMacros` expands OpenRTB substitution macros (`${AUCTION_PRICE}`,
  `${AUCTION_ID}`, …) in notice URLs. Expansion happens in `AdData.fromBid`
  where the settlement price is known. A notice fired with the literal macro is
  a dropped event on the bidder side.
- Notice semantics: `nurl` (win) fires as before; `burl` (billing) fires at the
  MRC-viewable impression in `ImpressionTracker`; `lurl` (loss, reason 102)
  fires for losing bids in `OpenRTBAdRepository` after the winner is chosen.

## Offline Tracking Queue (Trust Layer)

- `PersistentTrackingQueue` is the SDK's `TrackingClient`. Billing-relevant
  events (nurl, MRC-viewable burl, click/quartile beacons) are appended to a
  file-backed queue before the first send attempt and removed only after the
  transport confirms delivery — a lost billing event is lost publisher revenue
  and a server-side discrepancy.
- Failures retry with exponential backoff (5s base, 10min cap), bounded by 8
  attempts and 24h age. The queue survives process death: pending events are
  recovered and drained at next SDK init.
- Queue state is confined to `SdkExecutors.SINGLE`; redrains are scheduled on
  `SdkExecutors.SCHEDULER`. No locks are held during network sends.
- Server-issued URLs are HMAC-signed with a bounded TTL, so events older than
  the signature window may be rejected server-side even if delivered; the 24h
  age cap keeps the queue from retrying into certain rejection forever.

## Quality Report Loop (Trust Layer)

- When `AdNavigationGuard` blocks a navigation it reports through
  `AdQualityReporter` to the ad server's `POST /sdk/v1/quality-report`.
  Repeat-offender creatives are quarantined server-side, so a block on one
  device retires the creative fleet-wide instead of replaying on every device.
- The sink is installed by `ApexServices` at init (endpoint derived from the
  ad-server origin) and cleared on close. Reporting is best-effort,
  fire-and-forget on the IO executor: it must never affect rendering, and a
  failed report is only debug-logged.

## Open Measurement (OMID) Scaffolding

- `MeasurementDelegate` (sdk-core `di/`) is the optional-feature contract for
  buyer-verifiable viewability; `sdk-measurement` installs it via
  `MeasurementExtension.install()` — same pattern as wallet.
- The in-house `ImpressionTracker` stays the billing trigger; the OMID session
  exists so DSP-side measurement (DV360 Active View, IAS/DV/Moat) can verify
  the same impression. `BannerAdView` funnels the MRC-viewable event to both
  through one `onViewableImpression()` sink.
- **Signaling is honesty-gated**: `api=7` (OMID-1) and
  `source.ext.omidpn/omidpv` are added to bid requests only while
  `MeasurementDelegate.isReady()` is true. The scaffold delegate reports
  not-ready until the IAB Tech Lab OM SDK AAR is bundled (portal-distributed
  after partner registration — not on Maven Central) and `OMID_ACTIVE` is
  flipped. Never signal OMID without live measurement.
- Remaining steps to production are enumerated on `OmidMeasurementDelegate`:
  partner registration → bundle AAR → Omid.activate/Partner → script
  injection → HTML session wiring → certification → video (VAST
  AdVerifications) sessions.
