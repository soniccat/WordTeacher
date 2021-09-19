package com.aglushkov.wordteacher.androidApp.features.definitions.di

import com.aglushkov.wordteacher.androidApp.TabComposeModule
import com.aglushkov.wordteacher.di.AppComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(modules = [TabComposeModule::class])
interface TabComposeComponent {
    fun tabDecomposeComponent(): TabDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setWord(word: String?): Builder
        @BindsInstance fun setAppComponent(appComponent: AppComponent): Builder

        fun build(): TabComposeComponent
    }
}
