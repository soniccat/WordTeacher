plugins {
    id("kmmlib-convention")
    id("android-base-convention")
    id("resources")

    // for compose-jb - uncomment - start
//    id("org.jetbrains.compose")
    // for compose-jb - uncomment - end

    kotlin("plugin.serialization")
    // TODO: resolve that issue somehow, probably with extracting api/model stuff in a gradle module
//    >>> COMPOSE WARNING
//    >>> Project `shared` has `compose` and `kotlinx.serialization` plugins applied!
//    >>> Consider using these plugins in separate modules to avoid compilation errors
//    >>> Check more details here: https://github.com/JetBrains/compose-jb/issues/738
}

group = "com.aglushkov.wordteacher"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()

    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

android {
    // HACK: to solve "Configuration with name 'testApi' not found." and to support Arctic Fox
    // https://stackoverflow.com/questions/65372825/kotlin-multiplatform-configuration-issue
//    configurations {
//        create("androidTestApi")
//        create("androidTestDebugApi")
//        create("androidTestReleaseApi")
//        create("testApi")
//        create("testDebugApi")
//        create("testReleaseApi")
//    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

kotlin {
    jvm("desktop")
    android()

//     Block from https://github.com/cashapp/sqldelight/issues/2044#issuecomment-721299517.
//    val onPhone = System.getenv("SDK_NAME")?.startsWith("iphoneos") ?: false
//    if (onPhone) {
//        iosArm64("ios")
//    } else {
//        iosX64("ios")
//    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinxSerializationJson)
                implementation(libs.ktorCommonCore)
                implementation(libs.coroutinesCommon)
                implementation(libs.okio)
                implementation(libs.kotlinxDateTime)
                implementation(libs.logger)
                implementation(libs.sqlDelightRuntime)
                implementation(libs.uuid)

                //implementation(kotlin("stdlib"))
                // for compose-jb - uncomment - start
//                implementation(compose.runtime)
//                implementation(compose.foundation)
//                implementation(compose.material)
                // for compose-jb - uncomment - end

                implementation(libs.essentyParcelable)
                implementation(libs.essentryInstanceKeeper)
                implementation(libs.essentryStateKeeper)
                implementation(libs.decompose)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlinTest)
                implementation(libs.kotlinTestAnnotations)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidMaterial)
                implementation(libs.sqlDelightAndroidDriver)
                // for compose-jb - uncomment - start
//                implementation(compose.uiTooling)
//                implementation(compose.preview)
                // for compose-jb - uncomment - end
                implementation("org.apache.opennlp:opennlp-tools:1.9.2")
            }
        }
        val androidTest by getting {
            dependencies {
                implementation(libs.kotlinTestJUnit)
                implementation(libs.junit)
            }
        }
//        val iosMain by getting {
//            dependencies {
//                implementation(libs.ktoriOSClient)
//                implementation(libs.coroutinesCommon) /*{
//                    version {
//                        // HACK: to fix InvalidMutabilityException: mutation attempt of frozen kotlinx.coroutines.ChildHandleNode
//                        // during HttpClient initialization
//                        strictly(libs.versions.coroutines.get())
//                    }
//                }*/
//                implementation(libs.sqlDelightiOSDriver)
//            }
//        }
//        val iosTest by getting
        val desktopMain by getting {
            dependencies {
            }
        }
    }

//    cocoapods {
//        summary = "Nothing"
//        homepage = "https://aglushkov.com"
//
//        podfile = project.file("../iosApp/Podfile")
//        pod("Reachability","3.2")
//
//        ios.deploymentTarget = "11.0"
//    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}

resourcesPlugin {
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
