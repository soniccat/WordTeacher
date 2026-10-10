import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask

// TODO: add ktlint

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.sqlDelight) apply false
    alias(libs.plugins.mokoResources) apply false
    alias(libs.plugins.playServicesPlugin) apply false
    alias(libs.plugins.kapt) apply false
    alias(libs.plugins.daggerKsp) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.baselineprofile) apply false
}