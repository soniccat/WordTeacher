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
// HACK: commented to be able build the project with native.cocoapods plugin
//    ios {
//        binaries {
//            framework {
//                baseName = "shared"
//            }
//        }
//    }
    ios()
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(Deps.Mp.serializationCore)
                implementation(Deps.Ktor.commonCore)
                implementation(Deps.Coroutines.common)
                implementation(Deps.MokoResources.impl)
                implementation(Deps.MokoParcelize.impl)
                implementation(Deps.MokoGraphics.impl)
                implementation(Deps.mokoMvvm)
                implementation(Deps.okio)
                implementation(Deps.dateTime)
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
                implementation(Deps.Ktor.ios)
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

        // HACK: it's important to update iOS project Podfile with the same deps and call pod install as
        // the deps here aren't linked in the result shared framework and otherwise you'll have sth like
        // Reachability object isn't found for <any> architecture while linking step
        pod("Reachability","3.2")
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

val packForXcode by tasks.creating(Sync::class) {
    group = "build"
    val mode = System.getenv("CONFIGURATION") ?: "DEBUG"
    val sdkName = System.getenv("SDK_NAME") ?: "iphonesimulator"
    val targetName = "ios" + if (sdkName.startsWith("iphoneos")) "Arm64" else "X64"
    val framework = kotlin.targets.getByName<KotlinNativeTarget>(targetName).binaries.getFramework(mode)
    inputs.property("mode", mode)
    dependsOn(framework.linkTask)
    val targetDir = File(buildDir, "xcode-frameworks")
    from({ framework.outputDirectory })
    into(targetDir)
}
tasks.getByName("build").dependsOn(packForXcode)