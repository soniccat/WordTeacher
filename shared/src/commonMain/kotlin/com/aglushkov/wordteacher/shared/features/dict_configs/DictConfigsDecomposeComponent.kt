package com.aglushkov.wordteacher.shared.features.dict_configs

import com.aglushkov.wordteacher.shared.features.dict_configs.vm.DictConfigsVMImpl
import com.aglushkov.wordteacher.shared.general.FileOpenController
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy

class DictConfigsDecomposeComponent (
    componentContext: ComponentContext,
    configRepository: ConfigRepository,
    dslDictOpenController: FileOpenController,
    dictRepository: DictRepository,
    idGenerator: IdGenerator,
) : DictConfigsVMImpl(
    configRepository,
    dslDictOpenController,
    dictRepository,
    idGenerator,
), ComponentContext by componentContext {
    init {
        lifecycle.doOnDestroy {
            onCleared()
        }
    }
}
