package com.aglushkov.wordteacher.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.DefinitionsDisplayModeBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordDefinitionBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordDividerBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordExampleBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordHeaderBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordPartOfSpeechBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordSubHeaderBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordSynonymBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordTitleBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordTranscriptionBlueprint
import com.aglushkov.wordteacher.androidApp.general.ViewItemBinder
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
    @DefinitionsBinder
    fun createItemViewBinder(
        definitionsDisplayModeBlueprint: DefinitionsDisplayModeBlueprint,
        wordDefinitionBlueprint: WordDefinitionBlueprint,
        wordDividerBlueprint: WordDividerBlueprint,
        wordExampleBlueprint: WordExampleBlueprint,
        wordPartOfSpeechBlueprint: WordPartOfSpeechBlueprint,
        wordHeaderBlueprint: WordHeaderBlueprint,
        wordSubHeaderBlueprint: WordSubHeaderBlueprint,
        wordSynonymBlueprint: WordSynonymBlueprint,
        wordTitleBlueprint: WordTitleBlueprint,
        wordTranscriptionBlueprint: WordTranscriptionBlueprint
    ) = ViewItemBinder()
            .addBlueprint(definitionsDisplayModeBlueprint)
            .addBlueprint(wordDefinitionBlueprint)
            .addBlueprint(wordDividerBlueprint)
            .addBlueprint(wordExampleBlueprint)
            .addBlueprint(wordPartOfSpeechBlueprint)
            .addBlueprint(wordHeaderBlueprint)
            .addBlueprint(wordSubHeaderBlueprint)
            .addBlueprint(wordSynonymBlueprint)
            .addBlueprint(wordTitleBlueprint)
            .addBlueprint(wordTranscriptionBlueprint)

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
