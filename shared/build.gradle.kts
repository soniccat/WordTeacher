import org.gradle.kotlin.dsl.support.kotlinCompilerOptions
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)

    // from convention
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.mokoResources)

    alias(libs.plugins.kapt)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xopt-in=kotlin.time.ExperimentalTime")
    }

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm()

//    listOf(
//        iosX64(),
//        iosArm64(),
//        iosSimulatorArm64()
//    ).forEach { iosTarget ->
//        iosTarget.binaries.framework {
//            baseName = "Shared"
//            isStatic = true
//        }
//    }

    sourceSets {
        commonMain.dependencies {
            api(libs.logger)
            api(libs.okio)
            implementation(libs.sqlDelightRuntime)
            implementation(libs.kotlinxSerializationJson)
            implementation(libs.ktorCommonCore)
            api(libs.ktorLogging)
            api(libs.ktorContentEncoding)
            api(libs.mokoResourcesLib)
            api(libs.decompose)
            implementation(libs.ktorAuth)
            implementation(libs.kotlinxDateTime)
            implementation(libs.statelyCommon)
            implementation(libs.statelyConcurrency)
            implementation(libs.datastorePreference)
            val version = "0.5.0"

            // For parsing HTML
            implementation("com.mohamedrejeb.ksoup:ksoup-html:$version")

            // Only for encoding and decoding HTML entities
            implementation("com.mohamedrejeb.ksoup:ksoup-entities:$version")
            api(libs.uuid)
        }

        val composeSharedMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                api(libs.dagger)
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                api(compose.materialIconsExtended)
                api(compose.ui)
                api(compose.components.resources)
                api(compose.components.uiToolingPreview)
                implementation(libs.jsoup)
                implementation(libs.opennlp)
                api(libs.decomposeJetbrains)
                api(libs.mokoCompose)
            }
        }
        val androidMain by getting {
            dependsOn(composeSharedMain)
            dependencies {
                implementation(libs.androidx.material) // for colorOnPrimary in xmls
                implementation(libs.sqlDelightAndroidDriver)
                api(libs.androidx.activity.compose)
                api(libs.appmetrica)
                api(libs.ktorAndroidClient)
                api(libs.exoplayer)
                api(libs.exoplayerDatabase)
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.okioFakeFileSystem)
                implementation(libs.kotlinTestJUnit)
                implementation(libs.junit)
                implementation(libs.mockitoKotlin)
                implementation(libs.robolectric)
                implementation(libs.coroutinesCommonTest)
            }
        }
        val jvmMain by getting {
            dependsOn(composeSharedMain)
            dependencies {
                implementation(libs.ktorDesktop)
                implementation(libs.sqlDelightJvmDriver)
            }
        }
    }
}

android {
    namespace = "com.aglushkov.wordteacher.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}


// from convention

sqldelight {
    databases {
        create("MainDB") {
            packageName.set("com.aglushkov.wordteacher.maindb")
            schemaOutputDirectory.set(File("./src/commonMain/sqldelight/main/com/aglushkov/wordteacher/schemes"))
            srcDirs.setFrom("src/commonMain/sqldelight/main")
        }
        create("WordFrequencyDB") {
            packageName.set("com.aglushkov.wordteacher.wordfrequencydb")
            schemaOutputDirectory.set(File("./src/commonMain/sqldelight/wordfrequency/com/aglushkov/wordteacher/schemes"))
            srcDirs.setFrom("src/commonMain/sqldelight/wordfrequency")
        }
    }
}

multiplatformResources {
    resourcesPackage.set("com.aglushkov.wordteacher.shared.res") // required
    iosBaseLocalizationRegion = "en" // optional, default "en"
}

// For now you have to comment this to be able to build desktop... I'm investigating...
dependencies {
    configurations["kapt"](libs.daggerCompiler)
}

