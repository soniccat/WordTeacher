package com.aglushkov.wordteacher.android_app

import com.aglushkov.wordteacher.android_app.MainComposeModule
import com.aglushkov.wordteacher.android_app.di.AppComponent
import com.aglushkov.wordteacher.shared.features.MainDecomposeComponent
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
