import com.android.build.gradle.BaseExtension

configure<BaseExtension> {
    compileSdkVersion(35)
    buildToolsVersion = "35.0.0"

    defaultConfig {
        minSdk = 26
        targetSdk = 35
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}
