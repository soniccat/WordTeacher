package com.aglushkov.wordteacher.shared.general

data class AppInfo(val version: String, val osName: String)

fun AppInfo.getUserAgent() = "WordTeacher $version ($osName)"