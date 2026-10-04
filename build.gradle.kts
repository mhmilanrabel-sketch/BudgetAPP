// Top-level build file — shared config for all modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.secrets) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}