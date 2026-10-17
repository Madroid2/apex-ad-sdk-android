import java.io.File

plugins {
    alias(libs.plugins.android.library)
}

val apexSdkVersion = providers.gradleProperty("APEX_VERSION")
    .orElse("0.1.0-SNAPSHOT")
    .get()

// Resolves the host a debug build should target for the local ad server.
// Precedence:
//   1. `apex.adServerHost` in local.properties (per-machine, gitignored) — set
//      this to the Mac's LAN IP when testing on a physical Android device.
//   2. `10.0.2.2` — Android Emulator's stable alias for the host loopback.
// Defaulting to the emulator alias keeps an installed demo valid across Wi-Fi,
// VPN, and DHCP changes instead of compiling a temporary LAN address into it.
fun resolveDebugAdServerHost(): String {
    val override = File(rootDir, "local.properties").takeIf { it.exists() }
        ?.readLines()
        ?.firstOrNull { it.trim().startsWith("apex.adServerHost=") }
        ?.substringAfter("=")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    return override ?: "10.0.2.2"
}

android {
    namespace = "com.apexads.sdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        // Keep runtime/OpenRTB/OMID partner versioning aligned with the
        // published Maven coordinate. Release builds set -PAPEX_VERSION.
        buildConfigField("String", "SDK_VERSION", "\"$apexSdkVersion\"")
        buildConfigField("String", "SDK_NAME", "\"ApexAd\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "AD_SERVER_URL", "\"https://api.apexads.net/openrtb/v1/auction\"")
            buildConfigField("String", "TRACKING_URL",  "\"https://api.apexads.net\"")
        }
        debug {
            val host = resolveDebugAdServerHost()
            println("ApexAds debug build → ad server host = $host (override via apex.adServerHost in local.properties)")
            buildConfigField("String", "AD_SERVER_URL", "\"http://$host:8080/openrtb/v1/auction\"")
            buildConfigField("String", "TRACKING_URL",  "\"http://$host:8080\"")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    compileOnly(libs.annotation)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.json)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
