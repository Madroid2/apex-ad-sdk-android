plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    jacoco
}

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
        extensions.configure<com.android.build.gradle.LibraryExtension>("android") {
            testOptions {
                unitTests {
                    isIncludeAndroidResources = true
                    isReturnDefaultValues = true
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
