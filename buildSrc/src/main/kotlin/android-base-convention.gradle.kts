import com.android.build.gradle.BaseExtension

configure<BaseExtension> {
    compileSdkVersion(34)
    buildToolsVersion = "34.0.0"

    defaultConfig {
        minSdk = 21
        targetSdk = 34
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
