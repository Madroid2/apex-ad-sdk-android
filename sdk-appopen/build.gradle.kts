plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.apexads.sdk.appopen"
    compileSdk = 35
    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":sdk-interstitial"))
    compileOnly(libs.annotation)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
