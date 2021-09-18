
plugins {
    id("dev.icerock.mobile.multiplatform-resources")
}

multiplatformResources {
    multiplatformResourcesPackage = "com.aglushkov.wordteacher.shared.res" // required
    iosBaseLocalizationRegion = "en" // optional, default "en"
}
