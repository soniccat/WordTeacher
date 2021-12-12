package com.aglushkov.wordteacher.androidApp.features.definitions.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsFragment
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsAndroidVM
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
import dagger.BindsInstance
import dagger.Component

@FragmentComp
@Component(dependencies = [DefinitionsDependencies::class], modules = [DefinitionsModule::class])
public interface DefinitionsComponent {
    fun injectDefinitionsFragment(fragment: DefinitionsFragment)
    fun injectViewModelWrapper(fragment: DefinitionsAndroidVM)

    @Component.Builder
    interface Builder {
        @BindsInstance fun setVMState(state: DefinitionsVM.State): Builder
        @BindsInstance fun setVMWrapper(vmWrapper: DefinitionsAndroidVM): Builder

        fun setDeps(deps: DefinitionsDependencies): Builder
        fun build(): DefinitionsComponent
    }
}

interface DefinitionsDependencies {
    fun wordRepository(): WordDefinitionRepository
    fun connectivityManager(): ConnectivityManager
    fun idGenerator(): IdGenerator
    fun cardSetsRepository(): CardSetsRepository
}
