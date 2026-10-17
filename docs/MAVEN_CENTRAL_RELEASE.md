# Maven Central Release Runbook

The public Maven namespace is `net.apexsdk`, derived from the verified domain
`apexsdk.net`. Java source packages remain under `com.apexads`; Maven coordinates and Java
packages are independent identities.

## One-time namespace setup

1. Sign in to [Maven Central Portal](https://central.sonatype.com/).
2. Request the `net.apexsdk` namespace.
3. Copy the verification value issued by Central.
4. Add that value as a TXT record on the exact apex domain `apexsdk.net`.
5. Wait until the namespace status becomes `Verified` before creating a release.

Do not remove the TXT record until verification has completed. The website's GitHub Pages
records and the Maven TXT verification record can coexist.

## GitHub environment and secrets

Create a GitHub Actions environment named `maven-central`. Add an approval rule so a release
cannot be uploaded without an explicit reviewer decision. Store these environment secrets:

| Secret | Source |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Central Portal user token username |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal user token password |
| `SIGNING_KEY` | ASCII-armored private PGP key |
| `SIGNING_PASSWORD` | PGP key passphrase |

Never commit publishing tokens or the private PGP key. Publish the corresponding public key
to a supported public keyserver before staging the first release.

## Stage a release

1. Confirm the release commit is clean and all checks pass.
2. Create and push an immutable tag, for example `v0.1.0`.
3. Run the **Stage Maven Central Release** workflow and enter `0.1.0`.
4. The workflow checks out the matching tag, reruns verification, signs every publication,
   and uploads a Central deployment.
5. Inspect validation in Central Portal. Publish manually only after the artifact list,
   coordinates, dependency POMs, signatures, sources and documentation archives are correct.

Published Maven Central versions cannot be overwritten or deleted. Fixes require a new
version.

## Published modules

All release modules use the `net.apexsdk` group:

- `apex-sdk-core`
- `apex-sdk-banner`
- `apex-sdk-interstitial`
- `apex-sdk-native`
- `apex-sdk-conversational`
- `apex-sdk-video`
- `apex-sdk-inappbidding`
- `apex-sdk-appopen`
- `apex-sdk-wallet`
- `apex-sdk-measurement`
- `apex-sdk-integrity`
- `apex-adapter-admob`

`sdk-measurement` can be published as an integration contract, but OM SDK activation still
requires partner registration and the externally supplied IAB OM SDK artifact. Do not claim
certified OM measurement until the steps in `docs/OMID_CERTIFICATION.md` are complete.

## Local publication check

Use a snapshot version so signing is not required:

```bash
./gradlew publishToMavenLocal -PAPEX_VERSION=0.1.0-SNAPSHOT
```

Inspect `~/.m2/repository/net/apexsdk/`, then consume the artifacts from a clean Android
sample using `mavenLocal()` before creating the release tag.
