plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.apexads.demo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.apexads.demo"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":sdk-core"))
    implementation(project(":sdk-banner"))
    implementation(project(":sdk-interstitial"))
    implementation(project(":sdk-native"))
    implementation(project(":sdk-video"))
    implementation(project(":sdk-inappbidding"))
    implementation(project(":sdk-appopen"))
    implementation(project(":sdk-wallet"))

    implementation(libs.core.ktx)
    implementation(libs.material)          // provides Theme.Material3 for the Activity window
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coroutines.android)
    implementation(libs.timber)

    // Jetpack Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.activity)
    implementation(libs.navigation.compose)
    implementation(libs.compose.viewmodel)

    debugImplementation(libs.compose.ui.tooling)
}
