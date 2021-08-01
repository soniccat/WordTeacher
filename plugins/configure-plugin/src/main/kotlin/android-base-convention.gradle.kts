import com.android.build.gradle.BaseExtension

configure<BaseExtension> {
    compileSdkVersion(30)
    buildToolsVersion = "30.0.3"

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
    }
}
