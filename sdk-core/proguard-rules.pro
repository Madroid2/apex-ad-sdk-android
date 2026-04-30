# ApexAd SDK — sdk-core ProGuard rules
# These rules are embedded via consumerProguardFiles and applied to any app
# that depends on this module via AAR.

# Keep the public SDK API surface intact
-keep public class com.apexads.sdk.ApexAds { *; }
-keep public class com.apexads.sdk.ApexAdsConfig { *; }
-keep public class com.apexads.sdk.ApexAdsConfig$Builder { *; }
-keep public interface com.apexads.sdk.core.error.AdError { *; }
-keep public class com.apexads.sdk.core.error.** { *; }
-keep public class com.apexads.sdk.core.models.** { *; }
-keep public interface com.apexads.sdk.core.network.AdNetworkClient { *; }

# Moshi: keep generated adapter classes and annotated fields
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Prevent stripping of OpenRTB models (JSON serialization)
-keep class com.apexads.sdk.core.models.openrtb.** { *; }
