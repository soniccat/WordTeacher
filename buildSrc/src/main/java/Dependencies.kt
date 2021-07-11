object Versions {
    val minSdk = 21
    val targetSdk = 30
    val compileSdk = 30
    val buildToolVersion = "30.0.2"

    val kotlin = "1.5.10"
    val androidx_test = "1.2.0"
    val androidx_test_ext = "1.1.1"
    val android_gradle_plugin = "7.0.0-beta04"
    val junit = "4.13"
    val sqlDelight = "1.5.0"
    val ktor = "1.5.4"
    val coroutines = "1.4.2-native-mt"
    val serialization = "1.2.1"
    val lifecycle = "2.2.0"

    val compose_jetpack_version = "1.0.0-SNAPSHOT"
    val androidx_activity_compose_version = "1.3.0-SNAPSHOT"

    val mokoResources = "0.15.1"

    val decompose = "0.2.6"
}

object Deps {
    object Gradle {
        val kotlinClasspath = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
        val androidClasspath = "com.android.tools.build:gradle:${Versions.android_gradle_plugin}"
    }

    object MokoResources {
        val plugin = "dev.icerock.mobile.multiplatform-resources"
        val classpath = "dev.icerock.moko:resources-generator:${Versions.mokoResources}"
        val impl = "dev.icerock.moko:resources:${Versions.mokoResources}"
    }

    object MokoParcelize {
        val impl = "dev.icerock.moko:parcelize:0.5.0"
    }

    object MokoGraphics {
        val impl = "dev.icerock.moko:graphics:0.6.0"
    }

    object Mp {
        val serializationPlugin = "org.jetbrains.kotlin.plugin.serialization"
        val serializationClasspath = "org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}"
        val serializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.serialization}"
    }

    // TODO: sort by groups
    object Google {
        val appcompat = "androidx.appcompat:appcompat:1.2.0"
        val material = "com.google.android.material:material:1.2.1"
        val coreKtx = "androidx.core:core-ktx:1.2.0"
        val constraintLayout = "androidx.constraintlayout:constraintlayout:2.0.2"
        val recyclerView = "androidx.recyclerview:recyclerview:1.1.0"
        val viewModelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}"
        val lifecycleKtx = "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycle}"
        val fragments = "androidx.fragment:fragment-ktx:1.2.5"
    }

    object Compose {
        val foundation = "androidx.compose.foundation:foundation:${Versions.compose_jetpack_version}"
        val material = "androidx.compose.material:material:${Versions.compose_jetpack_version}"
        val tooling = "androidx.compose.ui:ui-tooling:${Versions.compose_jetpack_version}"
        val activity = "androidx.activity:activity-compose:${Versions.androidx_activity_compose_version}"
    }

    val okio = "com.squareup.okio:okio-multiplatform:2.9.0"
    val dateTime = "org.jetbrains.kotlinx:kotlinx-datetime:0.1.0"
    val junit = "junit:junit:${Versions.junit}"
    val logger = "com.github.aakira:napier:1.4.1"
    val floatingActionButtonSpeedDial = "com.github.leinardi:FloatingActionButtonSpeedDial:3.1.1"
    val uuid = "com.benasher44:uuid:0.3.0"

    object AndroidXTest {
        val core = "androidx.test:core:${Versions.androidx_test}"
        val junit = "androidx.test.ext:junit:${Versions.androidx_test_ext}"
        val runner = "androidx.test:runner:${Versions.androidx_test}"
        val rules = "androidx.test:rules:${Versions.androidx_test}"
    }

    object KotlinTest {
        val common = "org.jetbrains.kotlin:kotlin-test-common:${Versions.kotlin}"
        val annotations = "org.jetbrains.kotlin:kotlin-test-annotations-common:${Versions.kotlin}"
        val jvm = "org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}"
        val junit = "org.jetbrains.kotlin:kotlin-test-junit:${Versions.kotlin}"
    }

    object Coroutines {
        val common = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
        val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
        val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}"
    }

    object SqlDelight{
        val classpath = "com.squareup.sqldelight:gradle-plugin:${Versions.sqlDelight}"
        val plugin = "com.squareup.sqldelight"
        val runtime = "com.squareup.sqldelight:runtime:${Versions.sqlDelight}"
        val runtimeJdk = "com.squareup.sqldelight:runtime-jvm:${Versions.sqlDelight}"
        val iOSDriver = "com.squareup.sqldelight:native-driver:${Versions.sqlDelight}"
        val androidDriver = "com.squareup.sqldelight:android-driver:${Versions.sqlDelight}"
    }

    object Ktor {
        val commonCore = "io.ktor:ktor-client-core:${Versions.ktor}"
        val androidClient = "io.ktor:ktor-client-android:${Versions.ktor}"
        val iOSClient = "io.ktor:ktor-client-ios:${Versions.ktor}"
    }

    object Decompose {
        val decompose = "com.arkivanov.decompose:decompose:${Versions.decompose}"
        val jetpack = "com.arkivanov.decompose:extensions-compose-jetpack:${Versions.decompose}"
    }
}
