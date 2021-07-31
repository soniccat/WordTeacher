import com.android.build.gradle.BaseExtension

configure<BaseExtension> {
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
    }
}
