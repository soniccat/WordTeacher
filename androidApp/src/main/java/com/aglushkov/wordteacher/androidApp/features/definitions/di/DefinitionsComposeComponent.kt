package com.aglushkov.wordteacher.di

import com.aglushkov.wordteacher.androidApp.MainActivity
import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import dagger.BindsInstance
import dagger.Component

@FragmentComp
@Component(dependencies = [DefinitionsComposeDependencies::class], modules = [DefinitionsComposeModule::class])
public interface DefinitionsComposeComponent {
    fun injectActivity(fragment: MainActivity)

    @Component.Builder
    interface Builder {
        @BindsInstance fun setVMState(state: DefinitionsVM.State): Builder

        fun setDeps(deps: DefinitionsComposeDependencies): Builder
        fun build(): DefinitionsComposeComponent
    }
}

interface DefinitionsComposeDependencies {
    fun wordRepository(): WordDefinitionRepository
    fun connectivityManager(): ConnectivityManager
    fun idGenerator(): IdGenerator
}
