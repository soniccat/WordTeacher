package com.aglushkov.wordteacher.androidApp.features.definitions.di

import com.aglushkov.wordteacher.androidApp.MainComposeModule
import com.aglushkov.wordteacher.androidApp.TabComposeModule
import com.aglushkov.wordteacher.di.AppComponent
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(modules = [MainComposeModule::class])
interface MainComposeComponent {
    fun mainDecomposeComponent(): MainDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setAppComponent(appComponent: AppComponent): Builder

        fun build(): MainComposeComponent
    }
}
