import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

plugins {
    kotlin("multiplatform") // kotlin("jvm") doesn't work well in IDEA/AndroidStudio (https://github.com/JetBrains/compose-jb/issues/22)
    id("kotlin-kapt")
    id("org.jetbrains.compose")
}

kotlin {
    jvm {
        withJava()
    }
//    jvm {
//        withJava()
//    }
    sourceSets {
//        named("commonMain") {
//            dependencies {
//                implementation(compose.runtime)
//                implementation(compose.foundation)
//                implementation(compose.material)
//            }
//        }

        named("jvmMain") {
            dependencies {
                implementation(compose.desktop.common)
                implementation(compose.desktop.currentOs)
                implementation(libs.decompose)
                implementation(libs.decomposeJetbrains)
                implementation("com.google.dagger:dagger:2.38.1")
                implementation(project(":shared"))
            }
        }
    }

    dependencies {
        kapt {
            annotationProcessor("com.google.dagger:dagger-compiler:2.38.1")
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
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

            //modules("java.sql")

            windows {
                menuGroup = "Compose Examples"
                // see https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
                upgradeUuid = "BF9CDA6A-1391-46D5-9ED5-383D6E68CCEB"
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}