import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("kotlin-android-extensions")
    kotlin("native.cocoapods")
    id(Deps.Mp.serializationPlugin)
    id(Deps.MokoResources.plugin)
}
group = "com.aglushkov.wordteacher"
version = "1.0-SNAPSHOT"

repositories {
    gradlePluginPortal()
    google()
    jcenter()
    mavenCentral()
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}

kotlin {
    android()
    ios()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(Deps.Mp.serializationJson)
                implementation(Deps.Ktor.commonCore)
                implementation(Deps.Coroutines.common)
                implementation(Deps.MokoResources.impl)
                implementation(Deps.MokoParcelize.impl)
                implementation(Deps.MokoGraphics.impl)
                implementation(Deps.mokoMvvm)
                implementation(Deps.okio)
                implementation(Deps.dateTime)
                implementation(Deps.logger)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(Deps.KotlinTest.common)
                implementation(Deps.KotlinTest.annotations)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(Deps.Google.material)
            }
        }
        val androidTest by getting {
            dependencies {
                implementation(Deps.KotlinTest.junit)
                implementation(Deps.junit)
            }
        }
        val iosMain by getting {
            dependencies {
                implementation(Deps.Ktor.iOSClient)
                implementation(Deps.Coroutines.common) {
                    version {
                        // HACK: to fix InvalidMutabilityException: mutation attempt of frozen kotlinx.coroutines.ChildHandleNode
                        // during HttpClient initialization
                        strictly(Versions.coroutines)
                    }
                }
            }
        }
        val iosTest by getting
    }

    cocoapods {
        summary = "Nothing"
        homepage = "https://aglushkov.com"

        ios.deploymentTarget = "11.0"

        pod("Reachability","3.2")
        podfile = project.file("../iosApp/Podfile")
    }
}

android {
    compileSdkVersion(Versions.compileSdk)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(Versions.minSdk)
        targetSdkVersion(Versions.targetSdk)
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

multiplatformResources {
    multiplatformResourcesPackage = "com.aglushkov.wordteacher.shared.res" // required
    iosBaseLocalizationRegion = "en" // optional, default "en"
}
