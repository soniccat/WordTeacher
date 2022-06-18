import com.android.build.gradle.BaseExtension

configure<BaseExtension> {
    compileSdkVersion(32)
    buildToolsVersion = "32.0.0"

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(32)
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
