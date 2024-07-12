package com.aglushkov.wordteacher.shared.general

data class AppInfo(val version: String, val osName: String, val email: String)

fun AppInfo.getUserAgent() = "WordTeacher $version ($osName)"
fun AppInfo.getAppInfo() = "WordTeacher $version"