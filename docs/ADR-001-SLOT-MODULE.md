# ADR-001 — A `sdk-slot` module, not a runtime renderer registry

**Status:** Accepted (design only — no code written)
**Date:** 2026-08-03
**Scope:** `apex-ad-sdk-android`

---

## Context

Placement configuration now lives server-side. The ad server resolves a
placement from `imp.tagid` and returns its targeting and its render policy
(`ext.apex.render` — refresh interval, size, auto-close, mute, per-session cap)
on the bid response, so behaviour can be retuned from the publisher portal
without an app release.

That raises the obvious next question: if the server decides *how* a slot
behaves, how little integration code does the publisher need to write?

The goal is to collapse today's six format APIs (`BannerAd`, `NativeAd`,
`ConversationalAd`, `InterstitialAd`, `VideoAd`, `AppOpenAd`) into a much
smaller surface — ideally one inline primitive and one fullscreen primitive,
with the server choosing the format *within* each family.

Doing that requires one component to be able to render banner, native and
conversational creatives. Those are separate optional modules, and the SDK's
stated architecture is that publishers link only what they use, with no
dependency leakage. Two ways to reconcile that were considered.

## Options considered

### A. Runtime renderer registry via `ApexFeatureAccess`

A slot component in `sdk-core` looks up registered inline renderers at runtime,
the way `sdk-wallet` registers a `WalletDelegate` today. Each format module
registers itself on init; the slot degrades gracefully when a module is absent.

### B. A `sdk-slot` module that depends on the inline format modules

`sdk-slot` declares ordinary Gradle dependencies on `sdk-banner`, `sdk-native`
and `sdk-conversational`, and renders whichever the bid returns. Publishers who
want the thin integration add one module; publishers who do not are unaffected.

## Decision

**Option B.** Ship `sdk-slot` as an additive module. Do not extend the service
locator to cover format rendering.

## Rationale

**The service locator answers a question the build system already answers.**
`ApexFeatureAccess` earns its complexity for `sdk-wallet` because that is a
genuinely runtime-variable capability: whether a device can save a pass depends
on the device and the installed build, and the SDK must ask at request time.
That is not true of format renderers. Gradle already knows at compile time
whether `sdk-native` is on the classpath. Routing that through a runtime lookup
converts something explicit and statically verifiable into something implicit
and deferred — and moves the failure from a build error to a slot that silently
renders nothing in production. More machinery, worse failure mode, no new
information.

The rule this establishes: **use the service locator where the answer is
genuinely runtime-variable; use module dependencies where the build already
knows.**

**Two integration paths, neither compromised.** The dependency cost of `sdk-slot`
is only a flaw if it is imposed. It is not:

| Path | What the publisher writes | What they link | When it fits |
|---|---|---|---|
| Granular | `BannerAd` / `NativeAd` / `ConversationalAd` directly | only that format module | Method count matters; one known format; full control of the render |
| Thin | one `ApexAdSlot`, configured from the portal | `sdk-slot` + the inline format modules | Format or behaviour should change without an app release |

A publisher who only serves banners and cares about binary size keeps using
`BannerAd` and is unaffected by this ADR. A publisher who wants the dashboard to
switch a slot from banner to native accepts three modules on the classpath and
gets that in return. Both paths stay first-class, and `sdk-core` plus each format
module stay exactly as lean as they are today — which keeps the zero-leakage
claim in the README honest.

## Consequences

- `sdk-slot` is additive. No existing module changes, and no current integration
  breaks.
- A publisher using `ApexAdSlot` links banner, native and conversational even if
  only one ever serves. This is accepted, and must be stated plainly in the
  integration docs so it is a choice made with full information rather than a
  surprise found in a method-count report.
- `ApexFeatureAccess` stays scoped to genuinely optional capabilities
  (`WalletDelegate`, `MeasurementDelegate`). Future contributors should not
  extend it to rendering.

## Open question — the rewarded caveat

The proposed split is *inline* (occupies layout, publisher reserves space) and
*fullscreen* (interrupts a flow, publisher chooses the moment). Interstitial and
rewarded are both fullscreen, so a server could in principle swap between them.

**It must not.** Rewarded carries a value exchange the user has consented to. If
a publisher promises "watch for 50 coins" and the placement is silently switched
to a plain interstitial, either the app grants a reward for an ad that never
earned it, or the user is denied a reward they were promised. That is a contract
difference, not a rendering difference, and it is not the server's to change.

**Provisional position:** `ApexAds.fullscreen()` may choose freely among
*non-rewarded* fullscreen formats. Rewarded stays its own explicit primitive
with its own reward callback. To be confirmed before any fullscreen primitive is
built.

The same reasoning is why format *family* stays in the app at all. An inline slot
reserves layout space at build time — a server that changed 320×50 to 300×250
would cause layout shift or clipping — and a fullscreen ad interrupts a user flow
whose timing is app logic. AdMob and AppLovin MAX both keep format in code after
a decade of iteration; that is evidence, not conservatism.

## Status of the work

Landed:

- Server: placement render policy, bounded on write, returned in
  `ext.apex.render` on the bid response (`apex-ad-server` `62b18fa`).
- SDK: `RenderPolicy` parsing with independent on-device clamping
  (`apex-ad-sdk-android` `0a98686`).

Not built: `ApexAdSlot`, `ApexAds.fullscreen()`, and the `sdk-slot` module.
This ADR records the decision so that work starts from it rather than
re-litigating it.
