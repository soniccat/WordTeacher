import org.jetbrains.compose.compose

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.native.cocoapods")

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
    val onPhone = System.getenv("SDK_NAME")?.startsWith("iphoneos") ?: false
    if (onPhone) {
        iosArm64("ios")
    } else {
        iosX64("ios")
    }

    cocoapods {
        summary = "Nothing"
        homepage = "https://aglushkov.com"

        podfile = project.file("../iosApp/Podfile")
        pod("Reachability","3.2")

        ios.deploymentTarget = "11.0"
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
    kotlinOptions.jvmTarget = "11"
}
