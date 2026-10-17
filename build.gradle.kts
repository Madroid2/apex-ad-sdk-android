import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    // 0.34.x is the newest line compatible with this project's AGP 8.5.x baseline.
    id("com.vanniktech.maven.publish") version "0.34.0" apply false
    jacoco
}

val apexGroupId = "net.apexsdk"
val apexVersion = providers.gradleProperty("APEX_VERSION").orElse("0.1.0-SNAPSHOT")
val githubPackagesUrl = uri("https://maven.pkg.github.com/Madroid2/apex-ad-sdk-android")
val publishableArtifacts = mapOf(
    "sdk-core" to "apex-sdk-core",
    "sdk-banner" to "apex-sdk-banner",
    "sdk-interstitial" to "apex-sdk-interstitial",
    "sdk-native" to "apex-sdk-native",
    "sdk-conversational" to "apex-sdk-conversational",
    "sdk-video" to "apex-sdk-video",
    "sdk-inappbidding" to "apex-sdk-inappbidding",
    "sdk-appopen" to "apex-sdk-appopen",
    "sdk-wallet" to "apex-sdk-wallet",
    "sdk-measurement" to "apex-sdk-measurement",
    "sdk-integrity" to "apex-sdk-integrity",
    "adapters-admob" to "apex-adapter-admob"
)
val publicationMetadata = mapOf(
    "sdk-core" to ("ApexAd SDK Core" to "OpenRTB request, consent, network, lifecycle and tracking foundations for ApexAd Android integrations."),
    "sdk-banner" to ("ApexAd SDK Banner" to "Banner and MRECT rendering, viewability and click handling for ApexAd Android integrations."),
    "sdk-interstitial" to ("ApexAd SDK Interstitial" to "Fullscreen interstitial loading, readiness and rendering for ApexAd Android integrations."),
    "sdk-native" to ("ApexAd SDK Native" to "IAB OpenRTB Native 1.2 assets, rendering contracts and tracking for ApexAd Android integrations."),
    "sdk-conversational" to ("ApexAd SDK Conversational" to "Clearly disclosed sponsored suggestion cards for conversational Android surfaces."),
    "sdk-video" to ("ApexAd SDK Video" to "VAST video parsing, playback, skip, quartile and reward handling for ApexAd Android integrations."),
    "sdk-inappbidding" to ("ApexAd SDK In-App Bidding" to "In-app auction coordination and demand adapter contracts for ApexAd Android integrations."),
    "sdk-appopen" to ("ApexAd SDK App Open" to "Foreground lifecycle, cache and frequency controls for app-open advertising."),
    "sdk-wallet" to ("ApexAd SDK Wallet" to "Capability-negotiated Google Wallet advertising actions with ordinary creative fallback."),
    "sdk-measurement" to ("ApexAd SDK Measurement" to "Measurement integration contracts and lifecycle signals for ApexAd Android formats."),
    "sdk-integrity" to ("ApexAd SDK Integrity" to "Optional Play Integrity and request-signing signals for server-side supply validation."),
    "adapters-admob" to ("ApexAd SDK AdMob Adapter" to "AdMob mediation adapter for ApexAd Android demand."),
)

val businessLogicExcludes = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/com/apexads/demo/**",
    "**/com/apexads/sdk/adapters/**",
    "**/*Activity*.*",
    "**/*Application*.*",
    "**/*Screen*.*",
    "**/*View*.*",
    "**/*Adapter*.*",
    "**/*Listener*.*",
    "**/com/apexads/sdk/banner/**",
    "**/com/apexads/sdk/interstitial/**",
    "**/com/apexads/sdk/video/VideoAd*.*",
    "**/com/apexads/sdk/nativeads/NativeAd.class",
    "**/com/apexads/sdk/nativeads/NativeAd$*.class",
    "**/com/apexads/sdk/wallet/WalletAdExtension*.*",
    "**/com/apexads/sdk/wallet/WalletDelegateImpl*.*",
    "**/com/apexads/sdk/wallet/WalletPassManager*.*",
    "**/com/apexads/sdk/wallet/WalletResultActivity*.*",
    // Platform integration seams are exercised through protocol/contract tests;
    // Android Keystore and Play Integrity themselves require device/integration tests.
    "**/com/apexads/sdk/integrity/ApexIntegrityExtension*.*",
    "**/com/apexads/sdk/integrity/PlayIntegrityTrustDelegate*.*",
    "**/com/apexads/sdk/integrity/SessionKey*.*",
    "**/com/apexads/sdk/appopen/AppOpenAd*.*",
    "**/com/apexads/sdk/appopen/AppOpenAdManager*.*",
    "**/com/apexads/sdk/core/consent/**",
    "**/com/apexads/sdk/core/device/**",
    "**/com/apexads/sdk/core/tracking/**",
    "**/com/apexads/sdk/core/crashreporter/CrashDelivery*.*",
    "**/com/apexads/sdk/core/crashreporter/CrashReporter*.*",
    "**/com/apexads/sdk/core/network/FallbackAdNetworkClient*.*",
    "**/com/apexads/sdk/core/network/HttpAdNetworkClient*.*",
    "**/com/apexads/sdk/core/network/SdkExecutors*.*",
    "**/com/apexads/sdk/core/network/SdkHttpClient*.*",
    "**/com/apexads/sdk/core/request/**",
    "**/ui/**",
    "**/theme/**"
)

// Force a single androidx.annotation version across all subprojects.
// Without this, Gradle's consistent-resolution locks annotation to whatever
// version appcompat/constraintlayout etc. resolve at runtime (e.g. 1.6.0),
// which then conflicts with the 1.8.1 we declare on compile classpaths.
subprojects {
    group = apexGroupId
    version = apexVersion.get()

    apply(plugin = "jacoco")

    tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
        extensions.configure<org.gradle.testing.jacoco.plugins.JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }

    configurations.all {
        resolutionStrategy {
            force("androidx.annotation:annotation:1.8.1")
        }
    }

    plugins.withId("com.android.library") {
        if (name in publishableArtifacts) {
            apply(plugin = "com.vanniktech.maven.publish")
        }

        extensions.configure<com.android.build.gradle.LibraryExtension>("android") {
            testOptions {
                unitTests {
                    isIncludeAndroidResources = true
                    isReturnDefaultValues = true
                }
            }
        }

        if (name in publishableArtifacts) {
            extensions.configure<MavenPublishBaseExtension>("mavenPublishing") {
                configure(
                    AndroidSingleVariantLibrary(
                        variant = "release",
                        sourcesJar = true,
                        publishJavadocJar = true
                    )
                )
                coordinates(apexGroupId, publishableArtifacts.getValue(project.name), apexVersion.get())
                publishToMavenCentral()

                if (!apexVersion.get().endsWith("-SNAPSHOT")) {
                    signAllPublications()
                }

                pom {
                    val metadata = publicationMetadata.getValue(project.name)
                    name.set(metadata.first)
                    description.set(metadata.second)
                    inceptionYear.set("2026")
                    url.set("https://apexsdk.net")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }
                    developers {
                        developer {
                            id.set("Madroid2")
                            name.set("Madhur Lalit")
                            url.set("https://github.com/Madroid2")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/Madroid2/apex-ad-sdk-android.git")
                        developerConnection.set("scm:git:ssh://git@github.com/Madroid2/apex-ad-sdk-android.git")
                        url.set("https://github.com/Madroid2/apex-ad-sdk-android")
                    }
                }
            }

            extensions.configure<PublishingExtension>("publishing") {
                repositories {
                    maven {
                        name = "GitHubPackages"
                        url = githubPackagesUrl
                        credentials {
                            username = findProperty("gpr.user") as String?
                                ?: System.getenv("GITHUB_ACTOR")
                                ?: "Madroid2"
                            password = findProperty("gpr.key") as String?
                                ?: System.getenv("GITHUB_TOKEN")
                                ?: ""
                        }
                    }
                }
            }
        }
    }

    plugins.withId("com.android.application") {
        extensions.configure<com.android.build.gradle.AppExtension>("android") {
            testOptions {
                unitTests {
                    isIncludeAndroidResources = true
                    isReturnDefaultValues = true
                }
            }
        }
    }
}

tasks.register<JacocoReport>("jacocoDebugUnitTestReport") {
    group = "verification"
    description = "Generates an aggregate Jacoco report for debug unit tests."

    dependsOn(subprojects.mapNotNull { it.tasks.findByName("testDebugUnitTest") })

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val classFiles = files(subprojects.map { project ->
        project.layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")
    }).asFileTree.matching {
        exclude(businessLogicExcludes)
    }

    val kotlinClassFiles = files(subprojects.map { project ->
        project.layout.buildDirectory.dir("tmp/kotlin-classes/debug")
    }).asFileTree.matching {
        exclude(businessLogicExcludes)
    }

    classDirectories.setFrom(files(classFiles, kotlinClassFiles))
    sourceDirectories.setFrom(files(subprojects.map { project ->
        listOf(
            project.layout.projectDirectory.dir("src/main/java"),
            project.layout.projectDirectory.dir("src/main/kotlin")
        )
    }))
    executionData.setFrom(files(subprojects.flatMap {
        listOf(
            it.layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"),
            it.layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        )
    }))
}

tasks.register<JacocoCoverageVerification>("jacocoDebugCoverageVerification") {
    group = "verification"
    description = "Verifies aggregate business-logic coverage is at least 80%."

    dependsOn("jacocoDebugUnitTestReport")

    val classFiles = files(subprojects.map { project ->
        project.layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")
    }).asFileTree.matching {
        exclude(businessLogicExcludes)
    }

    val kotlinClassFiles = files(subprojects.map { project ->
        project.layout.buildDirectory.dir("tmp/kotlin-classes/debug")
    }).asFileTree.matching {
        exclude(businessLogicExcludes)
    }

    classDirectories.setFrom(files(classFiles, kotlinClassFiles))
    sourceDirectories.setFrom(files(subprojects.map { project ->
        listOf(
            project.layout.projectDirectory.dir("src/main/java"),
            project.layout.projectDirectory.dir("src/main/kotlin")
        )
    }))
    executionData.setFrom(files(subprojects.flatMap {
        listOf(
            it.layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"),
            it.layout.buildDirectory.file("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec")
        )
    }))

    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

tasks.register("verifyOmidCertificationReadiness") {
    group = "verification"
    description = "Fails until the IAB-issued OM SDK build and production partner identity are ready for certification."

    doLast {
        val failures = mutableListOf<String>()
        val partnerName = providers.gradleProperty("APEX_OMID_PARTNER_NAME").orNull?.trim().orEmpty()
        val omAars = fileTree("sdk-measurement/libs") { include("*.aar") }.files
        val delegateSource = file("sdk-measurement/src/main/java/com/apexads/sdk/measurement/OmidMeasurementDelegate.java").readText()

        if (partnerName.isEmpty()) {
            failures += "Set -PAPEX_OMID_PARTNER_NAME to the namespace assigned in the IAB Tech Lab Tools Portal."
        }
        if (omAars.size != 1) {
            failures += "Place exactly one IAB-issued Android OM SDK AAR in sdk-measurement/libs (found ${omAars.size})."
        }
        if (apexVersion.get().endsWith("SNAPSHOT", ignoreCase = true)) {
            failures += "Set a non-SNAPSHOT -PAPEX_VERSION matching the SDK build submitted for certification."
        }
        if (delegateSource.contains("OMID_ACTIVE = false")) {
            failures += "Complete the native OM SDK session integration and enable OMID_ACTIVE only after its device tests pass."
        }

        if (failures.isNotEmpty()) {
            throw GradleException("OMID certification is not ready:\n- ${failures.joinToString("\n- ")}")
        }
    }
}
