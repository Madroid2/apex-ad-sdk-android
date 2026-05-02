pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ApexAdSDK"

include(":sdk-core")
include(":sdk-banner")
include(":sdk-interstitial")
include(":sdk-native")
include(":sdk-video")
include(":sdk-inappbidding")
include(":sdk-appopen")
include(":sdk-wallet")
include(":adapters-admob")
include(":demo-app")
