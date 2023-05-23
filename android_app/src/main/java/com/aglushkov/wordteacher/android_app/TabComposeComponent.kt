package com.aglushkov.wordteacher.android_app

import com.aglushkov.wordteacher.android_app.TabComposeModule
import com.aglushkov.wordteacher.android_app.di.AppComponent
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
