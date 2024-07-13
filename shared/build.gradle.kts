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
}

kotlin {
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
            // put your Multiplatform dependencies here
            api(libs.logger)
            api(libs.okio)
            implementation(libs.sqlDelightRuntime)
            implementation(libs.kotlinxSerializationJson)
            implementation(libs.ktorCommonCore)
            api(libs.ktorLogging)
            api(libs.ktorContentEncoding)
            api(libs.mokoResourcesLib)
            api(libs.settings)
            api(libs.settingsCoroutines)
            api(libs.decompose)
            implementation(libs.ktorAuth)
            implementation(libs.kotlinxDateTime)
            implementation(libs.statelyCommon)
            implementation(libs.statelyConcurrency)
            implementation(libs.uuid)
        }

        val composeSharedMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                api(libs.dagger)
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                api(compose.ui)
                api(compose.components.resources)
                api(compose.components.uiToolingPreview)
                implementation(libs.jsoup)
                implementation(libs.opennlp)
                api(libs.decomposeJetbrains)
//                api(libs.decomposeJetbrains)
                api(libs.mokoCompose)
            }
        }
        val androidMain by getting {
            dependsOn(composeSharedMain)
            dependencies {
//                implementation(libs.androidMaterial)
                implementation(libs.androidx.material)
                implementation(libs.sqlDelightAndroidDriver)
                api(libs.androidx.activity.compose)
                api(libs.appmetrica)
            }
        }
        val androidUnitTest by getting {
            dependencies {
//                implementation(libs.kotlinTestJUnit)
//                implementation(libs.junit)
//                implementation(libs.mockitoKotlin)
//                implementation("org.robolectric:robolectric:4.11-beta-2")
//                implementation(libs.coroutinesCommonTest)
                implementation(libs.okioFakeFileSystem)
            }
        }
        val desktopMain by creating {
            dependsOn(composeSharedMain)
            dependencies {
//                implementation(libs.ktorDesktop)
//                implementation("org.apache.opennlp:opennlp-tools:1.9.4")
//                implementation("org.jsoup:jsoup:1.14.3")
//
                implementation(libs.sqlDelightJvmDriver)
//                implementation("org.xerial:sqlite-jdbc:3.42.0.0")
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
//    database("SQLDelightDatabase") {
//        packageName = "com.aglushkov.wordteacher.shared.data"
//        schemaOutputDirectory = File("./src/commonMain/sqldelight/data/com/aglushkov/wordteacher/schemes")
//        sourceFolders = listOf("sqldelight", "data")
//    }
}

multiplatformResources {
    resourcesPackage.set("com.aglushkov.wordteacher.shared.res") // required
    iosBaseLocalizationRegion = "en" // optional, default "en"
}


//

//plugins {
//    id("kmmlib-convention")
//    id("android-base-convention")
//    id("resources-convention")
//    id("sqldelight-convention")
//    id("dev.icerock.mobile.multiplatform-resources")
//    id("kotlin-kapt")
//}
//
//group = "com.aglushkov.wordteacher"
//version = "1.0-SNAPSHOT"
//
//repositories {
//    google()
//    mavenCentral()
//    gradlePluginPortal()
//
//    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
//}
//
//// For now you have to comment this to be able to build desktop... I'm investigating...
////dependencies {
////    add("kapt", libs.daggerCompiler)
////}
//
//kotlin {
//    jvmToolchain(17)
//
//    java {
//        jvmToolchain(17)
//    }
//
//    sourceSets {
//        commonMain.dependencies {
//            implementation(libs.kotlinxSerializationJson)
//            implementation(libs.ktorCommonCore)
//            api(libs.ktorLogging)
//            api(libs.ktorContentEncoding)
//            implementation(libs.ktorAuth)
//            implementation(libs.coroutinesCommon)
//            api(libs.okio)
//            implementation(libs.kotlinxDateTime)
//            api(libs.logger)
//            implementation(libs.sqlDelightRuntime)
//            implementation(libs.uuid)
//            implementation(libs.essentyParcelable)
//            implementation(libs.essentryInstanceKeeper)
//            implementation(libs.essentryStateKeeper)
//            implementation(libs.decompose)
//            implementation(libs.statelyCommon)
//            implementation(libs.statelyConcurrency)
//            implementation(compose.runtime)
//            api(libs.settings)
//            api(libs.settingsCoroutines)
//            api(libs.mokoResourcesLib)
//        }
//        commonTest.dependencies {
//            implementation(libs.kotlinTest)
//            implementation(libs.kotlinTestAnnotations)
//            implementation(libs.okioFakeFileSystem)
//            implementation(libs.coroutinesCommonTest)
//        }
//        val composeSharedMain by creating {
//            dependsOn(commonMain.get())
//            dependencies {
//                api(libs.dagger)
//                implementation(compose.runtime)
//                implementation(compose.foundation)
//                implementation(compose.material)
//                implementation(compose.preview)
//                implementation(compose.ui)
//                implementation(compose.uiTooling)
//                api(libs.decomposeJetbrains)
//                api(libs.mokoCompose)
//            }
//        }
//        val androidMain by getting {
//            dependsOn(composeSharedMain)
//            dependencies {
//                implementation(libs.androidMaterial)
//                implementation(libs.sqlDelightAndroidDriver)
//                api(libs.androidComposeActivity)
//                implementation("org.apache.opennlp:opennlp-tools:1.9.4")
//                implementation("org.jsoup:jsoup:1.14.3")
//                api(libs.appmetrica)
//            }
//        }
//        val androidUnitTest by getting {
//            dependencies {
//                implementation(libs.kotlinTestJUnit)
//                implementation(libs.junit)
//                implementation(libs.mockitoKotlin)
//                implementation("org.robolectric:robolectric:4.11-beta-2")
//                implementation(libs.coroutinesCommonTest)
//                implementation(libs.okioFakeFileSystem)
//            }
//        }
//        val desktopMain by getting {
//            dependsOn(composeSharedMain)
//            dependencies {
//                implementation(libs.ktorDesktop)
//                implementation("org.apache.opennlp:opennlp-tools:1.9.4")
//                implementation("org.jsoup:jsoup:1.14.3")
//
//                implementation(libs.sqlDelightJvmDriver)
//                implementation("org.xerial:sqlite-jdbc:3.42.0.0")
//            }
//        }
//    }
//}
//
//android {
//    namespace = "com.aglushkov.wordteacher.shared"
//
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_17
//        targetCompatibility = JavaVersion.VERSION_17
//    }
//}
