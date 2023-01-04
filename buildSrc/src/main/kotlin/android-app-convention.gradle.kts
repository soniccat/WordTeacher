import com.android.build.gradle.BaseExtension

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")

    // for compose-jb - uncomment - start
//    id("org.jetbrains.compose")
    // for compose-jb - uncomment - end
}
