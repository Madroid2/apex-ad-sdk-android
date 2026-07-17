# Production Trust Activation Runbook

This runbook is a release gate for the optional Apex Play Integrity trust layer. A future
operator or AI agent must not describe T2/T3 traffic as production-ready until every item
below is complete and an end-to-end lease has been observed from a Google Play-installed
release build.

## Inputs that must come from production systems

Do not invent, commit, or reuse demo values for these inputs:

- **Google Cloud project number**: the numeric project number, not the project ID. The
  Play Integrity API must be enabled for this project and the project must be linked to
  the publisher app in Google Play Console. SDK-provider deployments may instead use the
  Play SDK Console linking flow.
- **Publisher package name**: the release `applicationId` registered in Google Play.
- **App-signing SHA-256 certificate**: use the certificate for the key that Google Play
  uses to sign APKs delivered to devices. Do not substitute the upload-key certificate.
  Record every active certificate during a Play signing-key rotation.
- **Play Integrity server credentials**: service-account credentials, or a deliberately
  short-lived access token, for the Cloud project linked above. These belong only on Apex
  Ad Server; they must never be packaged in an Android app.
- **Trust lease secret**: a random production secret of at least 32 characters, stored in
  the deployment secret manager. Apex Ad Server and Apex Demand Platform must receive the
  exact same value. The Android SDK must never receive it.

Official setup references:

- [Enable and link the Play Integrity API](https://developer.android.com/google/play/integrity/setup)
- [Interpret Play Integrity verdicts](https://developer.android.com/google/play/integrity/verdicts)
- [Obtain an app-signing SHA-256 certificate](https://support.google.com/googleplay/android-developer/answer/16641489)

## Android release configuration

1. Add the optional `sdk-integrity` artifact. It requires API 23 or newer; `sdk-core`
   remains API 21 compatible.
2. Install the integrity extension before `ApexAds.init`, using the production numeric
   Cloud project number:

   ```kotlin
   ApexIntegrityExtension.install(PRODUCTION_CLOUD_PROJECT_NUMBER)
   ApexAds.init(this, productionConfig)
   ```

3. Ensure `productionConfig` points to the HTTPS Apex Ad Server deployment and uses the
   publisher token registered for the same release package.
4. Build the signed release app bundle and distribute it through a Google Play internal,
   closed, or production track. A sideloaded/debug build is not sufficient evidence for
   `PLAY_RECOGNIZED` production behavior.

## Required cross-service handoff

Give the Apex Ad Server deployment owner or agent:

| Value | Destination | Secret? |
|---|---|---:|
| Numeric Cloud project number | SDK configuration and deployment record | No |
| Release package name | Ad Server `publishers[].bundle` | No |
| Play app-signing SHA-256 certificate(s) | Ad Server `publishers[].signingCertSha256` | No |
| Publisher token | SDK config and matching Ad Server publisher entry | Yes |
| Play Integrity service-account credential location | Ad Server `GOOGLE_APPLICATION_CREDENTIALS` | Yes |
| Production trust lease secret | Ad Server and Demand `APEX_TRUST_LEASE_SECRET` | Yes |

Never copy the Play service-account file, lease secret, or raw publisher token into this
repository, its README, app resources, BuildConfig fields, logs, or CI artifacts.

## End-to-end acceptance gate

- [ ] The Play Integrity API is enabled and its Cloud project is linked to the release app.
- [ ] The number passed to `ApexIntegrityExtension.install` matches that linked project.
- [ ] Apex Ad Server starts in production mode with Play Integrity enabled and valid server
      credentials.
- [ ] The package and Play app-signing certificate in the decoded verdict match the
      registered publisher entry.
- [ ] Apex Ad Server and Apex Demand use the same lease secret without exposing it to the
      SDK.
- [ ] A Play-installed release on a qualifying physical device receives a T2 or T3 lease.
- [ ] A signed auction using that lease succeeds and reaches trusted demand.
- [ ] A replayed nonce, modified request body, stale envelope, wrong package, wrong
      certificate, and T1 billable event are rejected or restricted as configured.
- [ ] T1 fallback still avoids crashing the host app when Play Integrity is unavailable.

Capture only non-secret evidence: release version, package, Cloud project number, signing
certificate fingerprint, lease tier, timestamps, test results, and deployment revision.
Do not mark activation complete merely because unit tests or mocked verdicts pass.
