plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.apexads.sdk.inappbidding"
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
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
