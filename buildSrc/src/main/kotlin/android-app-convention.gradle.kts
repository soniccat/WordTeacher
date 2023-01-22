import java.io.FileInputStream
import java.util.*

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

// Signing file
var debugKeystoreProps: Properties? = null
val debugKeystorePropFile = file("${project.rootDir}/android_app/keystore.properties")
if (debugKeystorePropFile.exists()) {
    debugKeystoreProps = Properties().apply {
        load(FileInputStream(debugKeystorePropFile))
    }
}

android {
    signingConfigs {
        getByName("debug") {
            debugKeystoreProps?.let { props ->
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
            }
        }
    }
}
