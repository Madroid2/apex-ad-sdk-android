plugins {
    alias(libs.plugins.android.library)
    // Pure Java — no Kotlin plugin
}

android {
    namespace = "com.apexads.sdk.interstitial"
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
    api(project(":sdk-core"))
    api(project(":sdk-banner"))
    implementation(libs.appcompat)
    testImplementation(libs.junit)
}
