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
import com.aglushkov.wordteacher.androidApp.general.ViewItemBinder
import com.aglushkov.wordteacher.shared.features.RootDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.DefinitionsDecomposeComponent
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayMode
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsDisplayModeViewItem
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVMImpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import com.arkivanov.decompose.ComponentContext
import dagger.Module
import dagger.Provides
import javax.inject.Qualifier

@Module
class DaggerDefinitionsModule {
    @Provides
    fun createItemViewBinder(
        componentContext: ComponentContext,
        configuration: RootDecomposeComponent.ChildConfiguration.DefinitionConfiguration,
        connectivityManager: ConnectivityManager,
        wordDefinitionRepository: WordDefinitionRepository,
        idGenerator: IdGenerator,
    ) = DefinitionsDecomposeComponent(
            componentContext,
            configuration.word,
            connectivityManager,
            wordDefinitionRepository,
            idGenerator
        )
}
