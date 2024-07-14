package com.aglushkov.wordteacher.android_app.features.textaction.di

import com.aglushkov.wordteacher.android_app.di.AppComponent
import com.aglushkov.wordteacher.shared.features.textaction.TextActionDecomposeComponent
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component
import kotlinx.serialization.Serializable

@Component(modules = [TextActionModule::class])
interface TextActionComponent {
    fun textActionDecomposeComponent(): TextActionDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setAppComponent(appComponent: AppComponent): Builder
        @BindsInstance fun setConfig(config: Config): Builder

        fun build(): TextActionComponent
    }

    @Serializable
    data class Config(
        val text: String,
        val url: String?,
    )
}
