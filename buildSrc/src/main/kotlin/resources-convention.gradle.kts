import com.aglushkov.resource.ResourcesPlugin
import com.aglushkov.resource.ResourcesPluginExtension
import org.gradle.kotlin.dsl.configure

apply<ResourcesPlugin>()

configure<ResourcesPluginExtension> {
    multiplatformResourcesPackage = "com.aglushkov.wordteacher.shared.res" // required
    iosBaseLocalizationRegion = "en" // optional, default "en"
}
