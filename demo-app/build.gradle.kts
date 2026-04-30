plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
        viewBinding = true
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

    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.swiperefresh)
    implementation(libs.activity.ktx)
    implementation(libs.fragment.ktx)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.timber)
    implementation(libs.coroutines.android)
}
