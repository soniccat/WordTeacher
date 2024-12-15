package com.aglushkov.wordteacher.shared.general

data class AppInfo(
    val version: String,
    val osName: String,
    val email: String,
    val privacyPolicyUrl: String,
)

fun AppInfo.getUserAgent() = "Word Teacher $version ($osName)"
fun AppInfo.getAppInfo() = "Word Teacher $version"