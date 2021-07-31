enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "WordTeacher"

includeBuild("plugins/configure-plugin")
includeBuild("plugins/resources-plugin")
include(":androidApp")
include(":shared")
