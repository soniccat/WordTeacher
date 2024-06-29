package com.aglushkov.wordteacher.shared.features.webauth.di

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.webauth.WebAuthDecomposeComponent
import com.aglushkov.wordteacher.shared.general.TimeSource
import com.aglushkov.wordteacher.shared.general.oauth2.OAuth2Service
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [WebAuthDependencies::class], modules = [WebAuthModule::class])
interface WebAuthComponent {
    fun webAuthDecomposeComponent(): WebAuthDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setConfiguration(configuration: MainDecomposeComponent.ChildConfiguration.WebAuthConfiguration): Builder

        fun setDeps(deps: WebAuthDependencies): Builder
        fun build(): WebAuthComponent
    }
}

interface WebAuthDependencies {
    fun timeSource(): TimeSource
    fun oAuth2Service(): OAuth2Service
    fun analytics(): Analytics
}
