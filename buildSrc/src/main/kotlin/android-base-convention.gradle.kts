import com.android.build.gradle.BaseExtension

configure<BaseExtension> {
    compileSdkVersion(33)
    buildToolsVersion = "33.0.0"

    defaultConfig {
        minSdk = 21
        targetSdk = 33
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
