package com.aglushkov.wordteacher.androidApp.features.definitions.di

import com.aglushkov.wordteacher.androidApp.RootComposeModule
import com.aglushkov.wordteacher.di.AppComponent
import com.aglushkov.wordteacher.shared.features.RootDecomposeComponent
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(modules = [RootComposeModule::class])
interface RootComposeComponent {
    fun rootDecomposeComponent(): RootDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setWord(word: String?): Builder
        @BindsInstance fun setAppComponent(appComponent: AppComponent): Builder

        fun build(): RootComposeComponent
    }
}
