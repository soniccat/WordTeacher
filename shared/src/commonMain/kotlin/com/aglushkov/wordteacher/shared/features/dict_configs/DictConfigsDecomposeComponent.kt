package com.aglushkov.wordteacher.shared.features.dict_configs

import com.aglushkov.wordteacher.shared.analytics.Analytics
import com.aglushkov.wordteacher.shared.features.BaseDecomposeComponent
import com.aglushkov.wordteacher.shared.features.dict_configs.vm.DictConfigsVMImpl
import com.aglushkov.wordteacher.shared.general.FileOpenController
import com.aglushkov.wordteacher.shared.general.IdGenerator
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.dict.DictRepository
import com.aglushkov.wordteacher.shared.repository.remote_dict.RemoteDictRepository
import com.aglushkov.wordteacher.shared.usecase.dsl_dict_import_use_case.DslDictImportUseCase
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy

class DictConfigsDecomposeComponent (
    componentContext: ComponentContext,
    configRepository: ConfigRepository,
    dslDictOpenController: FileOpenController,
    dictRepository: DictRepository,
    remoteDictRepository: RemoteDictRepository,
    idGenerator: IdGenerator,
    analytics: Analytics,
    dslDictImportUseCase: DslDictImportUseCase,
) : DictConfigsVMImpl(
    configRepository,
    dslDictOpenController,
    dictRepository,
    remoteDictRepository,
    idGenerator,
    analytics,
    dslDictImportUseCase,
), ComponentContext by componentContext, BaseDecomposeComponent {
    override val componentName: String = "Screen_DictConfigs"

    init {
        baseInit(analytics)
    }
}
