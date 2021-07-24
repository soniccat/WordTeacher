plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

// To make it available as direct dependency
group = "com.aglushkov.plugins.deps"
version = "SNAPSHOT"

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("dependencies") {
            id = "dependencies"
            implementationClass = "com.aglushkov.plugins.deps.DependenciesPlugin"
        }
//        create("kmmdependencies") {
//            id = "com.aglushkov.kmmplugins"
//            implementationClass = "com.aglushkov.plugins.deps.MyKmmPlugin"
//        }
    }
}