package com.aglushkov.wordteacher.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.DefinitionsDisplayModeBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsFragment
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsVMWrapper
import com.aglushkov.wordteacher.androidApp.general.ViewItemBinder
import com.aglushkov.wordteacher.shared.features.definitions.repository.WordRepository
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import dagger.BindsInstance
import dagger.Component


@FragmentComp
@Component(dependencies = [DefinitionsDependencies::class], modules = [DefinitionsModule::class])
public interface DefinitionsComponent {
    fun getItemViewBinder(): ViewItemBinder

    fun injectDefinitionsFragment(fragment: DefinitionsFragment)
    fun injectViewModelWrapper(fragment: DefinitionsVMWrapper)

    @Component.Builder
    interface Builder {
        @BindsInstance fun setVMState(state: DefinitionsVM.State): Builder
        @BindsInstance fun setDefinitionsDisplayListener(listener: DefinitionsDisplayModeBlueprint.Listener): Builder

        fun setDeps(deps: DefinitionsDependencies): Builder
        fun build(): DefinitionsComponent
    }
}


interface DefinitionsDependencies {
    fun wordRepository(): WordRepository
    fun connectivityManager(): ConnectivityManager
    fun idGenerator(): IdGenerator
}