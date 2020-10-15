package com.aglushkov.wordteacher.di

import com.aglushkov.wordteacher.androidApp.di.AppComp
import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.DefinitionsDisplayModeBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordDefinitionBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordDividerBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordExampleBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordPartOfSpeechBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordSubHeaderBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordSynonymBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordTitleBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordTranscriptionBlueprint
import com.aglushkov.wordteacher.androidApp.general.ItemViewBinder
import com.aglushkov.wordteacher.shared.features.definitions.repository.WordRepository
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import dagger.Module
import dagger.Provides

@Module
class DefinitionsModule {
    @FragmentComp
    @Provides
    fun createItemViewBinder(
        definitionsDisplayModeListener: DefinitionsDisplayModeBlueprint.Listener
    ): ItemViewBinder {
        return ItemViewBinder()
            .addBlueprint(DefinitionsDisplayModeBlueprint(definitionsDisplayModeListener))
            .addBlueprint(WordDefinitionBlueprint())
            .addBlueprint(WordDividerBlueprint())
            .addBlueprint(WordExampleBlueprint())
            .addBlueprint(WordPartOfSpeechBlueprint())
            .addBlueprint(WordSubHeaderBlueprint())
            .addBlueprint(WordSynonymBlueprint())
            .addBlueprint(WordTitleBlueprint())
            .addBlueprint(WordTranscriptionBlueprint())
    }

    @FragmentComp
    @Provides
    fun viewModel(
        connectivityManager: ConnectivityManager,
        wordRepository: WordRepository,
        state: DefinitionsVM.State
    ): DefinitionsVM {
        return DefinitionsVM(connectivityManager, wordRepository, state)
    }
}