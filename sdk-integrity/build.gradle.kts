plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.apexads.sdk.integrity"
    compileSdk = 35
    defaultConfig {
        // Play Integrity 1.6.0 requires API 23. The optional module is isolated
        // so sdk-core continues to support API 21.
        minSdk = 23
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
    implementation(libs.play.integrity)
    compileOnly(libs.annotation)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.json)
    testImplementation(libs.robolectric)
}
