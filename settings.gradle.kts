
rootProject.name = "WordTeacher"

includeBuild("plugins/deps-plugin")
includeBuild("plugins/build-logic")
includeBuild("plugins/resources-plugin")
include(":androidApp")
include(":shared")
