import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

plugins {
    alias(libs.plugins.android.library)
}

// Resolves the host a debug build should target for the local ad server.
// Precedence:
//   1. `apex.adServerHost` in local.properties (per-machine, gitignored) — set
//      this when auto-detection picks the wrong NIC (VPN, multiple adapters).
//   2. This machine's current site-local IPv4 (192.168.x / 10.x / 172.16-31.x)
//      — works from a physical device on the same Wi-Fi, and survives DHCP
//      renewals because it is re-read on every build.
//   3. `10.0.2.2` — the Android emulator's alias for the host loopback.
// This is why a hardcoded IP is never needed: rebuilding always re-targets
// whatever address the Mac currently holds.
fun resolveDebugAdServerHost(): String {
    val override = File(rootDir, "local.properties").takeIf { it.exists() }
        ?.readLines()
        ?.firstOrNull { it.trim().startsWith("apex.adServerHost=") }
        ?.substringAfter("=")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
    if (override != null) return override

    return try {
        NetworkInterface.getNetworkInterfaces().toList()
            .asSequence()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .flatMap { it.inetAddresses.toList().asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { it.isSiteLocalAddress }
            ?.hostAddress
            ?: "10.0.2.2"
    } catch (e: Exception) {
        "10.0.2.2"
    }
}

android {
    namespace = "com.apexads.sdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "SDK_VERSION", "\"1.0.0\"")
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
