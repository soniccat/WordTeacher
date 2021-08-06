package com.aglushkov.wordteacher.desktopapp.features.definitions.di

import com.aglushkov.wordteacher.desktopapp.di.FragmentComp
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayMode
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayModeViewItem
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVMImpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import dagger.Module
import dagger.Provides
import javax.inject.Qualifier

@Module
class DefinitionsModule {
    @FragmentComp
    @Provides
    fun viewModel(
        connectivityManager: ConnectivityManager,
        wordDefinitionRepository: WordDefinitionRepository,
        idGenerator: IdGenerator,
        state: DefinitionsVM.State
    ): DefinitionsVM {
        return DefinitionsVMImpl(connectivityManager, wordDefinitionRepository, idGenerator, state)
    }
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DefinitionsBinder
