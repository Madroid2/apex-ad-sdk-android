-keep public class com.apexads.sdk.ApexAds { *; }
-keep public class com.apexads.sdk.ApexAdsConfig { *; }
-keep public class com.apexads.sdk.ApexAdsConfig$Builder { *; }
-keep public interface com.apexads.sdk.core.error.AdError { *; }
-keep public class com.apexads.sdk.core.error.** { *; }
-keep public class com.apexads.sdk.core.models.** { *; }
-keep public interface com.apexads.sdk.core.network.AdNetworkClient { *; }

-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

-dontwarn okhttp3.**
-dontwarn okio.**

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

-keep class com.apexads.sdk.core.models.openrtb.** { *; }
