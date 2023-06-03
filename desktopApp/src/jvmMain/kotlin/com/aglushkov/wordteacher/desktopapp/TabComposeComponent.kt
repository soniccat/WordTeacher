package com.aglushkov.wordteacher.desktopapp

import com.aglushkov.wordteacher.desktopapp.di.AppComponent
import com.aglushkov.wordteacher.shared.di.IsDebug
import com.aglushkov.wordteacher.shared.features.TabDecomposeComponent
import com.aglushkov.wordteacher.shared.features.settings.di.SettingsComponent
import com.arkivanov.decompose.ComponentContext
import dagger.BindsInstance
import dagger.Component

@Component(dependencies = [TabComposeComponentDependencies::class], modules = [TabComposeModule::class])
interface TabComposeComponent {
    fun tabDecomposeComponent(): TabDecomposeComponent

    @Component.Builder
    interface Builder {
        @BindsInstance fun setComponentContext(context: ComponentContext): Builder
        @BindsInstance fun setWord(word: String?): Builder
        @BindsInstance fun setAppComponent(appComponent: AppComponent): Builder

        fun setDeps(deps: TabComposeComponentDependencies): Builder
        fun build(): TabComposeComponent
    }
}

interface TabComposeComponentDependencies {
    @IsDebug
    fun isDebug(): Boolean
}
