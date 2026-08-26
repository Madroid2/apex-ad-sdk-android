plugins {
    alias(libs.plugins.android.library)
}

val omidPartnerName = providers.gradleProperty("APEX_OMID_PARTNER_NAME")
    .orElse("")
    .get()
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.apexads.sdk.measurement"
    compileSdk = 35
    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "OMID_PARTNER_NAME", "\"$omidPartnerName\"")
    }
    buildFeatures { buildConfig = true }
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
    // implementation(files("libs/omsdk-android-1.6.x-release.aar"))

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
