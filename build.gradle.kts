// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // Use the alias from the TOML file instead of "classpath"
    alias(libs.plugins.google.services) apply false
}
