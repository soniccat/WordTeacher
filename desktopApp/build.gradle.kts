import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.gradle.api.NamedDomainObjectContainer

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

plugins {
    kotlin("multiplatform")
    id("kotlin-kapt")
    id("org.jetbrains.compose")
    id("org.openjfx.javafxplugin") version "0.0.13"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

javafx {
    version = "19"
    modules("javafx.web", "javafx.swing")
}

//kotlinOptions {
//    jvmTarget = JavaVersion.VERSION_1_8.toString()
//}

kotlin {
    jvm {
        withJava()
        val javaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
        javaPluginConvention.sourceSets.all { javaSourceSet ->
            // HACK: adds Dagger generated classes
//            println("java sourceSet:" + javaSourceSet.name + " " + javaSourceSet.java.srcDirs.size)
            javaSourceSet.java.srcDir(project.rootDir.absolutePath + "/shared/build/generated/source/kapt/main/")
//            javaSourceSet.java.srcDirs.onEach { dir ->
//                println("srcDir file:" + dir.absolutePath)
//            }
//            javaSourceSet.allJava.onEach { file ->
//                println("java file:" + file.absolutePath)
//            }
            true
        }
    }

    sourceSets {
//        named("commonMain") {
//            dependencies {
//                implementation(compose.runtime)
//                implementation(compose.foundation)
//                implementation(compose.material)
//            }
//        }

        val jvmMain by getting {
            dependencies {
                implementation(project(":shared"))

                implementation(compose.desktop.common)
                implementation(compose.desktop.currentOs)
                implementation(libs.decompose)
                implementation(compose.runtime)
                implementation(compose.preview)
                implementation(compose.uiTooling)
                implementation(compose.ui)
                implementation(libs.coroutinesSwing)

                implementation(libs.dagger)

//                implementation("org.openjfx:javafx-base:20")
//                implementation("org.openjfx:javafx-web:20")
//                implementation("org.openjfx:javafx-swing:20")
            }
        }
    }
}

dependencies {
    add("kapt", libs.daggerCompiler)
}

//compose.desktop {
//    application {
//        mainClass = "com.aglushkov.wordteacher.desktopapp.MainKt"
//
//        nativeDistributions {
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
//            packageName = "WordTeacher"
//            packageVersion = "1.0.0"
//
//            modules("jdk.crypto.ec")
//
//            val iconsRoot = project.file("../common/src/jvmMain/resources/images")
//            macOS {
//                iconFile.set(iconsRoot.resolve("icon-mac.icns"))
//            }
//            windows {
//                iconFile.set(iconsRoot.resolve("icon-windows.ico"))
//                menuGroup = "Compose Examples"
//                // see https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
//                upgradeUuid = "18159995-d967-4CD2-8885-77BFA97CFA9F"
//            }
//            linux {
//                iconFile.set(iconsRoot.resolve("icon-linux.png"))
//            }
//        }
//    }
//}

compose.desktop {
    application {
        mainClass = "com.aglushkov.wordteacher.desktopapp.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "WordTeacher"
            packageVersion = "1.0.0"

            modules("java.sql")

            windows {
                menuGroup = "Compose Examples"
                // see https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
                upgradeUuid = "BF9CDA6A-1391-46D5-9ED5-383D6E68CCEB"
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}