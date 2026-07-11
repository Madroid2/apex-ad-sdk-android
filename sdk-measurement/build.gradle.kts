plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.apexads.sdk.measurement"
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
    compileOnly(libs.annotation)

    // OM SDK: the IAB Tech Lab AAR is distributed through the OM SDK portal after
    // partner registration (it is not on Maven Central). Once registered, drop the
    // AAR into sdk-measurement/libs and uncomment:
    // implementation(files("libs/omsdk-android-1.5.x-release.aar"))

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
