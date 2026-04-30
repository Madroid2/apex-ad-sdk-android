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
}
