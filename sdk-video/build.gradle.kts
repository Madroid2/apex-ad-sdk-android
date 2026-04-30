plugins {
    alias(libs.plugins.android.library)
    // Pure Java — no Kotlin plugin
}

android {
    namespace = "com.apexads.sdk.video"
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
    implementation(libs.appcompat)
    implementation(libs.exoplayer.core)
    implementation(libs.exoplayer.ui)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
