
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("kotlin-parcelize")
    kotlin("native.cocoapods")
    id(Deps.Mp.serializationPlugin)
    id(Deps.MokoResources.plugin)
    id(Deps.SqlDelight.plugin)
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

    // Block from https://github.com/cashapp/sqldelight/issues/2044#issuecomment-721299517.
    val onPhone = System.getenv("SDK_NAME")?.startsWith("iphoneos") ?: false
    if (onPhone) {
        iosArm64("ios")
    } else {
        iosX64("ios")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(Deps.Mp.serializationJson)
                implementation(Deps.Ktor.commonCore)
                implementation(Deps.Coroutines.common)
                implementation(Deps.MokoResources.impl)
                implementation(Deps.MokoParcelize.impl)
                implementation(Deps.MokoGraphics.impl)
                implementation(Deps.mokoMvvmCore)
                implementation(Deps.okio)
                implementation(Deps.dateTime)
                implementation(Deps.logger)
                implementation(Deps.SqlDelight.runtime)
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
                implementation(Deps.SqlDelight.androidDriver)
                implementation("org.apache.opennlp:opennlp-tools:1.9.2")
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
                implementation(Deps.SqlDelight.iOSDriver)
            }
        }
        val iosTest by getting
    }

    cocoapods {
        summary = "Nothing"
        homepage = "https://aglushkov.com"

        podfile = project.file("../iosApp/Podfile")
        pod("Reachability","3.2")

        ios.deploymentTarget = "11.0"
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

sqldelight {
    database("SQLDelightDatabase") {
        packageName = "com.aglushkov.wordteacher.shared.cache"
//        schemaOutputDirectory = File("/shared/src/commonMain/kotlin/com/aglushkov/wordteacher/db")
    }
}

// HACK: add sqlite3 lib in libraries deps
tasks.getByName("podspec").doLast {
    val podspec = file("${project.name.replace("-", "_")}.podspec")
    val newPodspecContent = podspec.readLines().map {
        if (it.contains("spec.libraries")) "    spec.libraries                = \"c++\", \"sqlite3\"" else it
    }
    podspec.writeText(newPodspecContent.joinToString(separator = "\n"))
}

// HACK: fix "Cannot inline bytecode built with JVM target 1.8 into bytecode that is being built with JVM target 1.6"
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}
