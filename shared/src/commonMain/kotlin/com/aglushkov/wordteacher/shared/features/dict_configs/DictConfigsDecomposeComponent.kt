package com.aglushkov.wordteacher.shared.features.dict_configs

import com.aglushkov.wordteacher.shared.features.dict_configs.vm.DictConfigsVMImpl
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.arkivanov.decompose.ComponentContext

class DictConfigsDecomposeComponent (
    componentContext: ComponentContext,
    configRepository: ConfigRepository,
    idGenerator: IdGenerator,
) : DictConfigsVMImpl(
    configRepository,
    idGenerator,
), ComponentContext by componentContext {
}
