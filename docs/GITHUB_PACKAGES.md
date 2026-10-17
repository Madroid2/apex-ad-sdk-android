# GitHub Packages Publishing

The SDK modules publish to GitHub Packages as Maven artifacts at:

```kotlin
maven {
    url = uri("https://maven.pkg.github.com/Madroid2/apex-ad-sdk-android")
}
```

The `Android Tests` workflow publishes on successful pushes to `main`, after unit
tests, Jacoco coverage verification, and instrumentation test compilation pass.
Published CI versions use:

```text
0.1.<github-run-number>-SNAPSHOT
```

## Published Artifacts

All coordinates use group `net.apexsdk`.

| Module | Artifact |
|---|---|
| `sdk-core` | `apex-sdk-core` |
| `sdk-banner` | `apex-sdk-banner` |
| `sdk-interstitial` | `apex-sdk-interstitial` |
| `sdk-native` | `apex-sdk-native` |
| `sdk-conversational` | `apex-sdk-conversational` |
| `sdk-video` | `apex-sdk-video` |
| `sdk-inappbidding` | `apex-sdk-inappbidding` |
| `sdk-appopen` | `apex-sdk-appopen` |
| `sdk-wallet` | `apex-sdk-wallet` |
| `sdk-measurement` | `apex-sdk-measurement` |
| `sdk-integrity` | `apex-sdk-integrity` |
| `adapters-admob` | `apex-adapter-admob` |

## Consumer Setup

GitHub's Maven registry requires credentials. In a consumer app, add the package
repository in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Madroid2/apex-ad-sdk-android")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .orNull
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .orNull
            }
        }
    }
}
```

Then depend on the modules needed by the app:

```kotlin
dependencies {
    implementation("net.apexsdk:apex-sdk-core:0.1.<run-number>-SNAPSHOT")
    implementation("net.apexsdk:apex-sdk-banner:0.1.<run-number>-SNAPSHOT")
    implementation("net.apexsdk:apex-sdk-interstitial:0.1.<run-number>-SNAPSHOT")
    implementation("net.apexsdk:apex-sdk-native:0.1.<run-number>-SNAPSHOT")
    implementation("net.apexsdk:apex-sdk-video:0.1.<run-number>-SNAPSHOT")
}
```

Use a GitHub token with `read:packages` for local consumption. In GitHub Actions,
`GITHUB_TOKEN` is enough when the workflow has package read access.
