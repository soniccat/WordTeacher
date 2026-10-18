import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)

    // from convention
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.sqlDelight)

    alias(libs.plugins.ksp)
}

compose.resources {
    publicResClass = true
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xopt-in=kotlin.time.ExperimentalTime")
    }

    android {
        namespace = "com.aglushkov.wordteacher.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTest {
            isIncludeAndroidResources = true
        }
        androidResources.enable = true

//        compilerOptions {
//            jvmTarget.set(JvmTarget.JVM_11)
//        }

        withJava() // to repair ksp task running (:kspAndroidMain)
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
            implementation(libs.kmpZip)
            api(libs.kmpZipOkio)
            implementation(libs.sqlDelightRuntime)
            implementation(libs.kotlinxSerializationJson)
            implementation(libs.ktorCommonCore)
            api(libs.ktorLogging)
            api(libs.ktorContentEncoding)
            api(libs.decompose)
            api("com.darkrockstudios:symspellkt:3.4.0")
            api(compose.components.resources)
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

            commonTest.dependencies {
                implementation(kotlin("test"))
            }
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
                implementation(libs.jsoup)
                implementation(libs.opennlp)
                api(libs.decomposeComposeExtensions)
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
                api(libs.androidx.ui.tooling)
            }
        }
        val androidHostTest by getting {
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
                api(compose.components.uiToolingPreview)
            }
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
        create("MisspellingDB") {
            packageName.set("com.aglushkov.wordteacher.misspellingdb")
            schemaOutputDirectory.set(File("./src/commonMain/sqldelight/misspelling/com/aglushkov/wordteacher/schemes"))
            srcDirs.setFrom("src/commonMain/sqldelight/misspelling")
        }
    }
}

// For now you have to comment this to be able to build desktop... I'm investigating...
dependencies {
    add("kspAndroid", libs.daggerCompiler)
}

