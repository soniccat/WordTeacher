package com.aglushkov.wordteacher.shared.di;

import androidx.compose.runtime.staticCompositionLocalOf
import javax.inject.Qualifier
import javax.inject.Scope

@Scope
annotation class AppComp

@Qualifier
annotation class BasePath

@Qualifier
annotation class DictPath

@Qualifier
annotation class SpaceHttpClient

@Qualifier
annotation class IsDebug

@Qualifier
annotation class Platform

@Qualifier
annotation class ApiBaseUrl

@Qualifier
annotation class ToggleUrl

@Qualifier
annotation class Email

@Qualifier
annotation class PrivacyPolicyUrl

@Qualifier
annotation class WordFrequencyPreparer

@Qualifier
annotation class WordFrequencyFileOpener

@Qualifier
annotation class DslFileOpener

val LocalIsDebug = staticCompositionLocalOf { false }

val LocalIsDarkTheme = staticCompositionLocalOf { false }