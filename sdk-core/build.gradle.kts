plugins {
    alias(libs.plugins.android.library)
    // No kotlin plugin — sdk-core is pure Java for maximum consumer compatibility
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
            // Production Apex Ad Server — update to your deployed domain before publishing.
            buildConfigField("String", "AD_SERVER_URL", "\"https://api.apexads.net/openrtb/v1/auction\"")
            buildConfigField("String", "TRACKING_URL",  "\"https://api.apexads.net\"")
        }
        debug {
            // Android emulator: 10.0.2.2 routes to host-machine localhost.
            // Run `docker compose up` in apex-ad-server/ and this works out of the box.
            buildConfigField("String", "AD_SERVER_URL", "\"http://10.0.2.2:8080/openrtb/v1/auction\"")
            buildConfigField("String", "TRACKING_URL",  "\"http://10.0.2.2:8080\"")
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
    // Zero third-party runtime dependencies — uses only Android platform APIs:
    //   org.json (bundled with Android) for JSON serialization
    //   java.net.HttpURLConnection for HTTP transport
    //   android.util.Log for logging

    // Annotation-only: no runtime code, no version conflict with consumer apps.
    compileOnly(libs.annotation)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.json)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
