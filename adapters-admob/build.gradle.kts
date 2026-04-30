plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.apexads.sdk.adapters.admob"
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
    api(project(":sdk-interstitial"))
    api(project(":sdk-video"))
    // AdMob provided by the consumer app — compileOnly avoids version conflicts
    compileOnly(libs.play.services.ads)
    compileOnly(libs.annotation)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
