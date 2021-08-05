package com.aglushkov.wordteacher.desktopapp.features.definitions.di

import com.aglushkov.wordteacher.desktopapp.di.FragmentComp
import com.aglushkov.wordteacher.desktopapp.features.definitions.views.DefinitionsFragment
import com.aglushkov.wordteacher.desktopapp.features.definitions.views.DefinitionsAndroidVM
import com.aglushkov.wordteacher.desktopapp.di.FragmentComp
import com.aglushkov.wordteacher.shared.repository.worddefinition.WordDefinitionRepository
import com.aglushkov.wordteacher.shared.features.definitions.vm.DefinitionsVM
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.general.connectivity.ConnectivityManager
import dagger.BindsInstance
import dagger.Component

@FragmentComp
@Component(dependencies = [DefinitionsDependencies::class], modules = [DefinitionsModule::class])
public interface DefinitionsComponent {
    @Component.Builder
    interface Builder {

        fun setDeps(deps: DefinitionsDependencies): Builder
        fun build(): DefinitionsComponent
    }
}

interface DefinitionsDependencies {
    fun wordRepository(): WordDefinitionRepository
    fun connectivityManager(): ConnectivityManager
    fun idGenerator(): IdGenerator
}
