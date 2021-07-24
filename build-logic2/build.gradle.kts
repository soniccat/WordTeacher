plugins {
    `kotlin-dsl`,
    id("dependencies")
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("androiddeps") {
            id = "com.aglushkov.plugins"
            implementationClass = "com.aglushkov.plugins.DependenciesPlugin"
        }
    }
}