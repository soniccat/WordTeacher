package com.aglushkov.wordteacher.shared.di;

import androidx.compose.runtime.staticCompositionLocalOf
import javax.inject.Qualifier
import javax.inject.Scope

@Scope
annotation class AppComp

@Qualifier
annotation class BasePath

@Qualifier
annotation class SpaceHttpClient

@Qualifier
annotation class IsDebug

@Qualifier
annotation class Platform

@Qualifier
annotation class ApiBaseUrl

val LocalIsDebug = staticCompositionLocalOf { false }

val LocalIsDarkTheme = staticCompositionLocalOf { false }