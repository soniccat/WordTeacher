plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("dependencies") {
            id = "com.aglushkov.plugins"
            implementationClass = "com.aglushkov.plugins.DependenciesPlugin"
        }
    }
}