# Velora proguard rules

# Keep Hilt generated components
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Coil
-keep class io.coil_kt.** { *; }
-dontwarn io.coil_kt.**

# Keep ApexAd SDK public API
-keep class com.apexads.sdk.** { public *; }
