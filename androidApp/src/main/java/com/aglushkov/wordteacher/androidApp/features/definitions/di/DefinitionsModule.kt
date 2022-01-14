package com.aglushkov.wordteacher.androidApp.features.definitions.di

import com.aglushkov.wordteacher.androidApp.di.FragmentComp
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.DefinitionsDisplayModeBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.DefinitionsDisplayModeBlueprintListener
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordDefinitionBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordDividerBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordExampleBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordHeaderBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordPartOfSpeechBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordSubHeaderBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordSynonymBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordTitleBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.blueprints.WordTranscriptionBlueprint
import com.aglushkov.wordteacher.androidApp.features.definitions.views.DefinitionsAndroidVM
import com.aglushkov.wordteacher.androidApp.general.RouterResolver
import com.aglushkov.wordteacher.androidApp.general.ViewItemBinder
import com.aglushkov.wordteacher.shared.features.definitions.vm.*
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.aglushkov.wordteacher.shared.repository.cardset.CardSetsRepository
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
        state: DefinitionsVM.State,
        connectivityManager: ConnectivityManager,
        wordDefinitionRepository: WordDefinitionRepository,
        cardSetsRepository: CardSetsRepository,
        idGenerator: IdGenerator,
    ): DefinitionsVM {
        return DefinitionsVMImpl(
            state,
            connectivityManager,
            wordDefinitionRepository,
            cardSetsRepository,
            idGenerator,
        )
    }

    @FragmentComp
    @Provides
    fun definitionsDisplayModeBlueprintListener(
        androidVM: DefinitionsAndroidVM
    ): DefinitionsDisplayModeBlueprintListener {
        return object : DefinitionsDisplayModeBlueprintListener {
            override fun onPartOfSpeechFilterClicked(item: DefinitionsDisplayModeViewItem) = androidVM.vm.onPartOfSpeechFilterClicked(item)
            override fun onPartOfSpeechFilterCloseClicked(item: DefinitionsDisplayModeViewItem) = androidVM.vm.onPartOfSpeechFilterCloseClicked(item)
            override fun onDisplayModeChanged(mode: DefinitionsDisplayMode) = androidVM.vm.onDisplayModeChanged(mode)
        }
    }
}

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DefinitionsBinder
