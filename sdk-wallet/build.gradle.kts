plugins {
    alias(libs.plugins.android.library)
    // Pure Java — no Kotlin plugin
}

android {
    namespace = "com.apexads.sdk.wallet"
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
    // play-services-wallet is an implementation dep here, so it does NOT
    // leak into sdk-core or any other module that only depends on sdk-core.
    implementation(libs.play.services.pay)
    compileOnly(libs.annotation)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
