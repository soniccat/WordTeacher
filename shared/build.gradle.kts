import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude

plugins {
    id("kmmlib-convention")
    id("android-base-convention")
    id("resources-convention")
    id("sqldelight-convention")
    id("dev.icerock.mobile.multiplatform-resources")
}

group = "com.aglushkov.wordteacher"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()

    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
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
                implementation(libs.essentyParcelable)
                implementation(libs.essentryInstanceKeeper)
                implementation(libs.essentryStateKeeper)
                implementation(libs.decompose)
                implementation(libs.statelyCommon)
                implementation(libs.statelyConcurrency)
                api(libs.settingsCoroutinesMt)
                api(libs.mokoResourcesLib)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlinTest)
                implementation(libs.kotlinTestAnnotations)
                implementation(libs.okioFakeFileSystem)
                implementation(libs.coroutinesCommonTest)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidMaterial)
                implementation(libs.sqlDelightAndroidDriver)
                implementation("org.apache.opennlp:opennlp-tools:1.9.4")
                implementation("org.jsoup:jsoup:1.14.3")

                // for compose-jb - uncomment - start
//                implementation(compose.runtime)
//                implementation(compose.foundation)
//                implementation(compose.material)
                // for compose-jb - uncomment - end
            }
        }
        val androidTest by getting {
            dependencies {
                implementation(libs.kotlinTestJUnit)
                implementation(libs.junit)
                implementation(libs.mockitoKotlin)
                implementation("org.robolectric:robolectric:4.7.3")
                implementation(libs.coroutinesCommonTest)
                implementation(libs.okioFakeFileSystem)
            }
        }
        val iosMain by getting {
            dependencies {
                implementation(libs.ktoriOSClient)
                implementation(libs.coroutinesCommon)/* {
                    version {
                        // HACK: to fix InvalidMutabilityException: mutation attempt of frozen kotlinx.coroutines.ChildHandleNode
                        // during HttpClient initialization
                        strictly(libs.versions.coroutines.get())
                    }
                }*/
                implementation(libs.sqlDelightiOSDriver)
            }
        }
        val iosTest by getting
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktorDesktop)
                // for compose-jb - uncomment - start
//                implementation(compose.runtime)
//                implementation(compose.foundation)
//                implementation(compose.material)
                // for compose-jb - uncomment - end

//                implementation(compose.uiTooling)
//                implementation(compose.preview)
                implementation(libs.sqlDelightJvmDriver)
            }
        }
    }
}
