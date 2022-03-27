import com.android.build.gradle.BaseExtension

configure<BaseExtension> {
    compileSdkVersion(31)
    buildToolsVersion = "31.0.0"

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(31)
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
