plugins {
    alias(libs.plugins.android.library)
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
    compileOnly(libs.annotation)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.json)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
