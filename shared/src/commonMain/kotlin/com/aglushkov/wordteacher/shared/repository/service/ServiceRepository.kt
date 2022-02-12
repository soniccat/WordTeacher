package com.aglushkov.wordteacher.shared.repository.service

import com.aglushkov.wordteacher.shared.general.extensions.forward
import com.aglushkov.wordteacher.shared.general.resource.Resource
import com.aglushkov.wordteacher.shared.general.resource.merge
import com.aglushkov.wordteacher.shared.repository.config.Config
import com.aglushkov.wordteacher.shared.repository.config.ConfigRepository
import com.aglushkov.wordteacher.shared.repository.config.ServiceMethodParams
import com.aglushkov.wordteacher.shared.service.WordTeacherWordService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ServiceRepository(
    val configRepository: ConfigRepository,
    val connectParamsStatRepository: ConfigConnectParamsStatRepository,
    val serviceFactory: WordTeacherWordServiceFactory
) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val services = combine(configRepository.flow, connectParamsStatRepository.flow) { a, b -> a to b }
        .map { (configs, configConnectStats) ->
            configs.copyWith(
                if (configs is Resource.Loaded) {
                    configs.data.mapNotNull { safeConfig ->
                        createWordTeacherWordService(safeConfig)
                    }
                } else {
                    emptyList()
                }
            )
        }.stateIn(scope, SharingStarted.Eagerly, Resource.Uninitialized())

    private fun createWordTeacherWordService(it: Config): WordTeacherWordService? {
        // TODO: filter connectParams with connectParamsStat
        val connectParams = it.connectParams.first()
        return serviceFactory.createService(it.type, connectParams, ServiceMethodParams(it.methods))
    }
}