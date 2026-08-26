# Apex Ad Tech

## Android SDK-led programmatic stack

Built independently by Madhur Lalit | Technical brief | August 2026

### What I built

A modular Android monetization SDK backed by a Go SSP/auction engine and a unified
advertiser/publisher platform. The work explores how an SDK-owned supply path can
combine standards-based rendering, privacy-aware signals, verifiable request trust,
action-oriented formats, and AI agents that remove operational bottlenecks without
placing AI in the auction hot path.

### Apex Ad SDK for Android

- **Formats and standards:** Banner/MRECT, Interstitial, IAB Native 1.2, VAST 4.0
  rewarded video, App Open, In-App Bidding, and MRAID 3.0 rich media over OpenRTB 2.6.
- **Publisher-safe architecture:** API 21+ modular AARs, zero third-party runtime
  dependencies in `sdk-core`, lifecycle-owned ad objects, consumer ProGuard rules,
  GitHub Packages publication, and 40 unit/instrumentation test source files.
- **Privacy and signal integrity:** consent-aware GAID/LAT handling, TCF mobile keys,
  GPP, US Privacy, COPPA/LMT, consent-gated first-party cohorts, real WebView user agent,
  device/network signals, and `schain` propagation.
- **Trust and accounting:** optional Play Integrity leases, Android Keystore P-256
  request signing, replay-resistant lifecycle calls, separate win/bill/loss handling,
  OpenRTB macro expansion, and a persistent retry queue for tracking events.
- **Integration surface:** native Android demo app, AdMob mediation adapter, in-app
  bidding token flow, and a working React Native bridge for the Android SDK.

### Differentiated formats built on the SDK

- **Conversational Ads for AI assistants:** a newly introduced, clearly disclosed
  programmatic Native format designed for AI-assistant chats, a surface most existing
  Ad Tech SDKs do not expose through a dedicated ad integration. Only registered
  taxonomy nodes are sent; raw chat or search text never leaves the app.

- **Wallet Pass Ads:** `Save to Google Wallet` as an optional action layer on standard
  MRECT, interstitial, or Native inventory, with normal display fallback when Wallet is
  unavailable.
- **Intent-to-Action Native:** a standard Native 1.2 creative can carry a disclosed,
  executable action negotiated against capabilities actually installed in the app.
- **Server-side placement control:** publishers can change floor, taxonomy, journey
  stage, disclosure, and allowed actions without shipping an app update. The SDK keeps
  a capability veto so server policy cannot promise an action the app cannot execute.

### AI agents for Ad Tech flow bottlenecks

- **Creative Generation Agent:** turns a landing page or product brief into
  multi-format creative drafts, reducing production and campaign-setup delays.
- **Creative Review Agent:** applies versioned policy, brand/domain, landing-page, and
  asset checks with audit evidence and manual escalation, reducing approval delays.
- **Campaign Copilot:** diagnoses delivery, budget, bidding, creative, and tracking
  issues from platform evidence, reducing buyer and AdOps troubleshooting time.
- **Publisher Copilot:** guides placement and format choices, then triages SDK,
  Gradle/runtime, crash, ANR, and integration issues with escalation to engineering.

The agents operate in the control plane with approval gates and audit trails. Auctions
and rendering remain deterministic.

### End-to-end system

```text
Android / React Native SDK
        |
        | OpenRTB 2.6 + privacy, capability, and trust signals
        v
Go Ad Server / SSP
        | parallel demand fan-out, quality gates, dynamic floors,
        | publisher registry, app-ads.txt verification, auction receipts
        v
Apex Platform
        | advertiser and publisher portals; Creative Generation and Review Agents;
        | Campaign and Publisher Copilots
        v
Apex Intelligence
        | privacy-minimized data contracts, gated pCTR training,
        | model registry, human approval, shadow-first scoring
```

The latency boundary is deliberate: auctions and rendering remain deterministic. AI
generation, review, support, and campaign explanation run in the control plane.

### Current readiness

**Working today:** modular SDK and demo app; end-to-end SDK-to-auction-to-demand flow;
standard display/native/video rendering; privacy and trust contracts; publisher
onboarding and placement control; advertiser creative/review workflow; audit and
reconciliation foundations.

**Before broad production distribution:** complete IAB-issued OM SDK integration and
certification; production fleet telemetry and remote kill switch; Maven Central and API
compatibility guarantees; full device/instrumentation, leak, ANR, parser-fuzz, and load
testing; external privacy/security review. OMID is scaffolded and honesty-gated but is
not advertised as active before certification.

### Key differentiators from commonly used Ad Tech products

1. **SDK-direct supply:** a first-party mobile supply path with clearer ownership,
   signals, and auction transparency.
2. **Conversational Ads:** a dedicated, disclosed programmatic Native format for
   AI-assistant chats that most Ad Tech SDKs do not yet expose.
3. **AI workflow agents:** Creative Generation, Creative Review, Campaign Copilot, and
   Publisher Copilot remove production, approval, operations, and support bottlenecks.

### GitHub reference for technical deep dives

Android SDK: https://github.com/Madroid2/apex-ad-sdk-android
