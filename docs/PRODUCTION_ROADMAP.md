# ApexAd SDK — Production Go-To-Market Roadmap

**Status:** Draft for review · **Scope:** `apex-ad-sdk-android` · **Audience:** SDK team + prospective SDK-technology partners (e.g. Smadex)

This document is the gap analysis and phased roadmap for taking ApexAd from its current
state — a feature-rich, standards-based showcase SDK — to a production-grade,
go-to-market advertising SDK that a demand-side partner such as **Smadex** could adopt
as the foundation of its own SDK program.

---

## 1. Where the SDK stands today

### Implemented and genuinely strong

| Area | What exists |
|---|---|
| **Standards core** | OpenRTB 2.6 request/response graph (hand-rolled `org.json`), VAST 4.0 (inline + wrapper), MRAID 3.0 bridge, IAB Native 1.2, US Privacy string, COPPA flag |
| **Formats** | Banner (incl. MRECT), Interstitial, Native, VAST Rewarded Video, App Open, In-App Bidding token, Google Wallet Pass layer |
| **Architecture** | Modular AARs with clean dependency boundaries; custom MVVM (no AndroidX ViewModel); typed `ApexServices` + feature-only `ServiceLocator`; zero 3P runtime deps in `sdk-core` |
| **Differentiators** | Wallet pass ads as a feature layer on existing formats; first-party declarative cohorts (`user.data[].segment[]`, TCF Purpose-4 gated); in-house Sentry-envelope crash reporting; App Open as a native format |
| **Demand plumbing** | `WaterfallAdNetworkClient` with priority sources, honest no-fill (`nbr=2`), TTL ad cache, win-notice firing |
| **Quality gates** | 25 unit-test classes, 80% JaCoCo line-coverage gate in CI, consumer ProGuard rules per module, GitHub Actions test + publish pipeline |
| **Distribution** | Maven publications for all 9 artifacts to GitHub Packages, POMs with license/SCM |

### The honest gap summary

The SDK implements the *ad-serving* half of a production SDK very well. What is missing
is the *operational* half: verifiable measurement, current-generation privacy
compliance, real device identity, fleet observability, hardened distribution, and the
ecosystem surface (mediation adapters, wrappers, certification) that publishers and
DSP partners audit before shipping an SDK inside their apps.

---

## 2. Gap analysis — what's missing, and why it matters to a DSP partner

Priorities: **P0** = go-to-market blocker (a partner's SDK-review checklist fails
without it) · **P1** = competitive necessity within the first releases · **P2** =
scale/ecosystem. Effort: S (< 1 wk) / M (1–3 wks) / L (> 3 wks).

### 2.1 Device identity & signal fidelity — P0

| Gap | Current state | Why it matters | Effort |
|---|---|---|---|
| **Real GAID retrieval** | `DeviceInfoProvider` hardcodes `advertisingId = null`, `limitAdTracking = false` | Without `device.ifa` a DSP cannot frequency-cap, retarget, attribute, or price users — eCPMs collapse and most performance demand won't bid. Needs `play-services-ads-identifier` (scoped, or a direct binder call to preserve zero-dep core), honoring LAT/zeroed IDs | M |
| **App Set ID fallback** | Absent | The sanctioned per-developer ID when GAID is unavailable; expected by modern bidders | S |
| **Real WebView user agent** | Hardcoded `Chrome/120` UA string | A fabricated UA mismatching the device is a classic IVT/fraud flag at exchanges and verification vendors; must use `WebSettings.getDefaultUserAgent()` (cached off-main-thread) | S |
| **Geo enrichment** | No `device.geo` beyond defaults | Country/region at minimum (SIM/network country, no location permission required); location only with permission + consent | S |
| **`device.w/h/ppi`, `hwv`, carrier (`mccmnc`)** | Partial | Standard bidder-side features for pricing | S |

### 2.2 Measurement & verification — P0

| Gap | Current state | Why it matters | Effort |
|---|---|---|---|
| **Open Measurement SDK (OMID) integration** | In-house `ImpressionTracker` only | This is the single biggest buyer-facing gap. Brand demand (and every major DSP, Smadex included) requires OM-verified viewability; an unverifiable in-house tracker carries no weight with buyers or the MRC. Integrate OM SDK for WebView (display/MRAID) and native video sessions, register with IAB Tech Lab, pass compliance | L |
| **VAST 4.2/4.3 upgrade** | VAST 4.0 | 4.2 adds `<AdVerifications>` (OMID-native), SIMID for interactive creatives, and the macro set DSPs actually use | M |
| **Tracking-event completeness** | Win notice + quartiles | `burl` (billing) vs `nurl` (win) separation, loss notices (`lurl`) with loss-reason codes, `${AUCTION_PRICE}` / `${AUCTION_LOSS}` macro expansion — DSPs need loss signals to tune bidding | M |
| **Click measurement** | Basic | Click-through vs click-tracking separation, viewable-impression vs rendered-impression distinction | S |

### 2.3 Privacy & regulatory compliance — P0

| Gap | Current state | Why it matters | Effort |
|---|---|---|---|
| **TCF 2.2** | TCF 2.0-era key reading | Google requires a TCF 2.2-certified CMP for EEA traffic since Nov 2023; 2.2 changed legitimate-interest handling and vendor disclosure. Update parsing/semantics and document the CMP contract | M |
| **IAB GPP support** | Absent (`IABGPP_*` keys unread) | GPP is how US state privacy laws (CA/VA/CO/CT/UT + newer) travel in bid requests (`regs.gpp`, `regs.gpp_sid`); US demand increasingly requires it, and USP v1 is deprecated | M |
| **Consent enforcement (not just forwarding)** | Signals forwarded to bid request | SDK must *behave* differently without consent: suppress GAID, skip cohorts (done), disable crash-report PII, honor `gdprApplies` with no TC string. An enforcement matrix + tests is what auditors ask for | M |
| **Google Play Data Safety & SDK disclosure docs** | Absent | Every publisher must fill the Data Safety form; SDKs without a canonical disclosure page get dropped. Also register in the **Google Play SDK Index / SDK Console** | S–M |
| **Android Privacy Sandbox readiness** | Absent | Roadmap item, but partners ask now: Topics API as contextual signal, Attribution Reporting API for post-GAID attribution, SDK Runtime compatibility audit (no reflection helps here), Protected Audience evaluation | L (phased) |
| **DSA transparency** | Absent | OpenRTB 2.6 `ext.dsa` (DSA Transparency) is now required for EU display demand; render "why this ad" affordance | M |

### 2.4 Supply-chain integrity & ad quality — P0/P1

| Gap | Current state | Why it matters | Effort |
|---|---|---|---|
| **`source.ext.schain` (SupplyChain object)** | Absent | DSPs filter inventory without a complete supply chain; table stakes for any exchange path. P0 | S |
| **app-ads.txt / sellers.json alignment** | Absent | Documentation + server-side story so buyers can validate authorized sellers. P0 (docs) | S |
| **Creative safety hardening** | `AdNavigationGuard`, `AdUrlHandler` exist | Extend to: auto-redirect/auto-store-open blocking inside WebView, `shouldOverrideUrlLoading` allow-list, blocking `intent://` abuse, MRAID feature gating by permission. P1 | M |
| **Parser fuzz/robustness testing** | Unit tests only | VAST XML and Native JSON parsers face hostile input in production; add fuzz corpus + malformed-creative test suite. P1 | M |
| **Certificate pinning (optional)** | Absent | Optional pinning for SDK↔ad-server traffic; expected in security reviews. P2 | S |

### 2.5 Reliability, performance & observability — P0/P1

| Gap | Current state | Why it matters | Effort |
|---|---|---|---|
| **SDK telemetry pipeline** | Crash reporting only | A GTM SDK needs fleet metrics: init latency, request latency, fill rate, render rate, render latency, expiry rate, error taxonomy — sampled, batched, privacy-safe. This is also the data a partner like Smadex needs to run supply. P0 | L |
| **Remote config + kill switch** | Cohort rules only (internal) | Ability to disable a format/feature/version fleet-wide without app updates is the #1 operational safeguard publishers ask about. P0 | M |
| **Event batching + offline queue** | Fire-and-forget pixels | Tracking events need a persisted queue with retry/backoff and batching — lost billing events are lost revenue. P0 | M |
| **R8/minification enabled for release** | `isMinifyEnabled = false` | Ship shrunk, optimized release AARs with verified consumer rules; publish binary-size budget per module (publishers audit SDK size). P0 | S |
| **ANR-safe initialization** | Synchronous init | Async init with completion listener; strict main-thread budget (measure with Macrobenchmark); publishers reject SDKs that ANR at startup. P1 | M |
| **WebView renderer crash isolation** | Not handled | Implement `onRenderProcessGone` so a crashed renderer never takes the host app down. P1 | S |
| **Memory-leak CI** | None | LeakCanary-instrumented test app in CI; ad views/activities are classic leak sources. P1 | S |
| **Baseline Profiles + Macrobenchmark** | None | Startup/jank budgets, published numbers. P2 | M |
| **Network layer resiliency** | `HttpURLConnection`, basic | Configurable timeouts, retry/backoff policy per request class, connection reuse audit; optional Cronet module for HTTP/2+QUIC without breaking the zero-dep core. P1 | M |
| **16 KB page-size & SDK Runtime compliance statement** | Implicitly fine (pure JVM) | Turn the implicit property into a tested, documented guarantee. P2 | S |

### 2.6 Monetization features & format parity — P1

| Gap | Current state | Why it matters | Effort |
|---|---|---|---|
| **Banner auto-refresh** | Manual `load()` on expiry | Viewability-gated auto-refresh (30–120 s, server-controllable) is standard and materially lifts publisher revenue | M |
| **Adaptive banners** | Fixed sizes only | Google-popularized anchored/inline adaptive sizes are now publisher default expectations | M |
| **Rewarded interstitial** | Absent | Fast-growing format; cheap to add on the interstitial + video base | M |
| **Native video** | Native = static assets | IAB Native 1.2 video asset support riding the existing VAST engine | M |
| **Multi-size / multi-format requests** | One format per request | `imp[]` with multiple sizes/formats raises fill and eCPM | M |
| **Real mediation adapters** | AdMob adapter real; MAX/LevelPlay are mocks | Ship production AppLovin MAX and Unity LevelPlay custom adapters + a Prebid Mobile-compatible signal path; the bidding token flow exists but needs real S2S token exchange (signed, short-TTL tokens) | L |
| **Price floors & currency** | Static `bidFloor` | Server-driven floors, `cur` handling, floor optimization hooks | M |
| **Ad pods / VAST buffet** | Single ad | Lower priority for mobile; evaluate at Phase 3 | L |

### 2.7 Distribution, API stability & developer experience — P0/P1

| Gap | Current state | Why it matters | Effort |
|---|---|---|---|
| **Maven Central publishing** | GitHub Packages only (requires auth token) | GitHub Packages needs a PAT even for public artifacts — unacceptable integration friction for GTM. Publish signed artifacts to Maven Central with a BOM | M |
| **API stability contract** | None | Adopt binary-compatibility validation (metalava or Kotlin BCV), `@Deprecated` policy, semver discipline, documented public-API surface. Partners building on the SDK (Smadex's case) need this above almost everything | M |
| **Reference docs site** | README + 4 docs | Dokka/Javadoc site, per-format integration guides, migration guides, changelog, versioned docs | M |
| **In-app integration validator** | None | A "mediation debugger"-style diagnostic screen (SDK version, config, consent state, adapter status, test ads) — the single biggest support-cost reducer; MAX proved the pattern | M |
| **Test mode & test placements** | `debugFakeFill` (debug builds only) | Formal test-device registration + always-fill test creatives in release builds, so publishers can validate before go-live | S |
| **Sample apps per feature + wrapper plugins** | One demo app | Unity, Flutter, React Native wrappers decide deals in gaming/commerce verticals. P2 but plan the C-friendly API surface now | L |
| **CI hardening** | Unit tests run; instrumentation tests only *compile* | Run instrumentation tests on emulator matrix, release automation from tags (signed, changelog-checked), Dokka publish, size-diff check per PR | M |

### 2.8 Certification & trust — P1/P2

- **IAB Tech Lab OM SDK certification** (follows 2.2) — P1.
- **MRC accreditation path** for viewable impressions — long-term (P2) but the OM SDK work is the prerequisite.
- **Google Play SDK Console** enrollment: crash/ANR visibility to publishers, version deprecation messaging — P1.
- **SOC 2 / security whitepaper** for the server side + SDK threat model doc — P2, but Smadex-grade partners ask during due diligence.

---

## 3. Phased roadmap

### Phase 0 — Foundation hardening (≈ 4–6 weeks)
*Goal: nothing in the SDK is fake, fragile, or unshippable.*

1. Real GAID + LAT + App Set ID (scoped identifier module or binder call), real WebView UA, geo/carrier enrichment.
2. Enable R8 for release AARs; verify consumer rules; establish per-module size budget in CI.
3. Event queue: persisted, batched, retrying tracking delivery (`nurl`/`burl`/`lurl` + macros).
4. `schain` support + app-ads.txt documentation.
5. Maven Central publishing with signed artifacts + BOM; keep GitHub Packages for snapshots.
6. CI: run instrumentation tests on emulators; tag-driven release automation.

### Phase 1 — Compliance & measurement (≈ 6–10 weeks)
*Goal: pass any publisher's or DSP partner's SDK review checklist.*

1. OM SDK integration (display + video), IAB certification submission.
2. TCF 2.2 semantics, GPP (`regs.gpp`/`gpp_sid`), consent-enforcement matrix + tests.
3. VAST 4.2 upgrade (`AdVerifications`, macro set).
4. Remote config service + kill switch; SDK telemetry v1 (init/request/render metrics, sampled).
5. ANR-safe async init, `onRenderProcessGone`, LeakCanary CI, network resiliency pass.
6. Data Safety disclosure doc, SDK Index/Console registration, DSA `ext.dsa` support.

### Phase 2 — Monetization & DX (≈ 8–12 weeks)
*Goal: revenue parity with incumbent SDKs and best-in-class integration experience.*

1. Viewability-gated banner auto-refresh; adaptive banners; rewarded interstitial; native video.
2. Production MAX + LevelPlay adapters; hardened bidding-token flow (signed, short-TTL); Prebid Mobile compatibility.
3. Multi-size/multi-format requests; server-driven floors and currency.
4. In-app integration validator + formal test mode; Dokka docs site + versioned guides; API stability gate (BCV) in CI.
5. Creative-safety hardening + parser fuzz suite.

### Phase 3 — Ecosystem & scale (ongoing)
*Goal: platform reach and institutional trust.*

1. Unity / Flutter / React Native wrappers; iOS parity SDK (shared spec, AdAttributionKit/SKAN).
2. Privacy Sandbox: Topics signals, Attribution Reporting, SDK Runtime compatibility; Protected Audience evaluation.
3. MRC accreditation track; SOC 2 for server side; security whitepaper.
4. Ad pods, additional exchange/network adapters, CTV evaluation.

---

## 4. The Smadex angle — what this SDK offers a DSP, and what it must add first

Smadex is a mobile-first DSP; its interest in ApexAd is as a **foundation for its own
SDK program** (SDK-direct supply and/or a rendering SDK for its demand). Framed that way:

### What ApexAd already gives Smadex

- **Exchange-agnostic OpenRTB 2.6 client** — the endpoint is config-driven, so pointing the SDK at the Smadex bidder is a configuration exercise, not a rewrite. SDK-direct supply shortens the supply path (fewer resold hops, better margins, cleaner `schain` once added).
- **Zero third-party runtime dependencies** — the property DSP-owned SDKs struggle most to retrofit. No OkHttp/Gson/DI version conflicts with publisher apps means lower integration friction and support cost from day one.
- **Modular AARs with strict boundaries** — Smadex could ship only the formats it sells; optional modules (wallet) prove the pattern of scoped dependencies.
- **First-party cohort layer** — privacy-gated, declarative, remotely configurable audience segments delivered as standard `user.data[].segment[]`. For a DSP this is directly monetizable: audience-based bidding on SDK supply without a third-party data dependency.
- **Wallet Pass ads** — a differentiated, performance-oriented retail format (save-to-wallet as an ad outcome) that no incumbent SDK offers; a concrete story for Smadex's commerce/performance advertisers.
- **In-house crash reporting + custom MVVM** — operational self-sufficiency; no Sentry/Firebase contract needed to run the fleet.

### What Smadex would require before adopting (the deal-breaker list)

In priority order — these map to Phase 0/1 above:

1. **Real `device.ifa`** — a DSP cannot buy (or sell its buyers) supply without device identity + LAT semantics.
2. **OM SDK certification** — Smadex's brand demand and its buyers' verification vendors (IAS/DV/Moat) only trust OM sessions.
3. **TCF 2.2 + GPP + consent enforcement** — a DSP carries the regulatory exposure; forwarding stale-generation signals is a liability.
4. **`schain` + loss notices + `burl`/`AUCTION_PRICE` macros** — bidder-side optimization and billing integrity depend on these.
5. **Fleet telemetry + remote kill switch** — running an SDK across third-party apps without observability or an emergency brake is operationally unacceptable.
6. **Maven Central + semver + API-stability guarantees** — if Smadex builds on these APIs, breaking changes are their outage.
7. **White-label readiness** — today the ad-server/tracking URLs are release-build `BuildConfig` constants and branding is compiled in; a partner build needs runtime/config-driven endpoints, a rebrandable namespace/artifact plan, and a partner build pipeline. (Small, but must be deliberate.)

### Positioning summary

> ApexAd's differentiation (zero-dep core, wallet format, cohorts, modularity) is real
> and is exactly what a DSP building an SDK cannot easily buy elsewhere. The gaps are
> almost entirely in the *operational trust layer* — identity, verified measurement,
> current-gen privacy, observability, and distribution hygiene. Phases 0–1 close every
> deal-breaker; Phases 2–3 make the SDK competitive with MAX/LevelPlay on revenue
> features and ecosystem reach.

---

## 5. Success metrics per phase

| Phase | Exit criteria |
|---|---|
| 0 | Non-null `ifa` on GMS devices; R8 release AARs ≤ agreed size budget; zero lost tracking events in airplane-mode test; artifacts resolvable from Maven Central without credentials |
| 1 | OM SDK certification submitted; TCF 2.2/GPP fields present and enforcement matrix tests green; kill switch demonstrated fleet-wide in < 5 min; telemetry dashboard shows init/fill/render for demo fleet |
| 2 | Fill-rate and eCPM parity benchmark vs AdMob on test inventory; MAX/LevelPlay adapters pass network certification; integration validator ships in demo app; API surface locked by BCV in CI |
| 3 | One wrapper (Unity or Flutter) GA; iOS spec parity doc; Privacy Sandbox compatibility report published |
