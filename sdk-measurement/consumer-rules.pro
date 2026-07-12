# Keep the OM SDK service classes once the IAB AAR is bundled — OMID resolves
# its JS bridge and verification vendor classes by name.
-keep class com.iab.omid.** { *; }
