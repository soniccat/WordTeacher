package com.aglushkov.wordteacher.androidApp.features.textaction.di

import android.os.Parcelable
import com.aglushkov.wordteacher.di.AppComponent
import com.aglushkov.wordteacher.shared.features.textaction.TextActionDecomposeComponent
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component
import kotlinx.parcelize.Parcelize

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

    @Parcelize
    data class Config(
        val text: CharSequence
    ): Parcelable
}
