
rootProject.name = "WordTeacher"

includeBuild("plugins/deps-plugin")
includeBuild("plugins/configure-plugin")
includeBuild("plugins/resources-plugin")
include(":androidApp")
include(":shared")
