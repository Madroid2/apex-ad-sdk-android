plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

// Force a single androidx.annotation version across all subprojects.
// Without this, Gradle's consistent-resolution locks annotation to whatever
// version appcompat/constraintlayout etc. resolve at runtime (e.g. 1.6.0),
// which then conflicts with the 1.8.1 we declare on compile classpaths.
subprojects {
    configurations.all {
        resolutionStrategy {
            force("androidx.annotation:annotation:1.8.1")
        }
    }
}
