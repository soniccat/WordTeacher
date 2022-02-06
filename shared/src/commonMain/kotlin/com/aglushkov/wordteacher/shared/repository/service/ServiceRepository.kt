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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ServiceRepository(
    val configRepository: ConfigRepository,
    val connectParamsStatRepository: ConfigConnectParamsStatRepository,
    val serviceFactory: WordTeacherWordServiceFactory
) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val stateFlow = MutableStateFlow<Resource<List<WordTeacherWordService>>>(Resource.Uninitialized())
    val flow = stateFlow

    val services: Resource<List<WordTeacherWordService>>
        get() {
            return stateFlow.value
        }

    init {
        // update on configRepository or connectParamsStatRepository change
        scope.launch {
            configRepository.flow
                .combine(connectParamsStatRepository.flow) { a, b ->
                    a.merge(b)
                }.map {
                    val services: MutableList<WordTeacherWordService> = mutableListOf()
                    if (it is Resource.Loaded) {
                        val config = it.data.first
                        val connectParamsStat = it.data.second

                        if (config != null && connectParamsStat != null) {
                            val filteredServices = config.mapNotNull { safeConfig ->
                                createWordTeacherWordService(safeConfig)
                            }
                            services.addAll(filteredServices)
                        }
                    }

                    it.copyWith(services.toList())
                }.forward(stateFlow)
        }
    }

    private fun createWordTeacherWordService(it: Config): WordTeacherWordService? {
        // TODO: filter connectParams with connectParamsStat
        val connectParams = it.connectParams.first()
        return serviceFactory.createService(it.type, connectParams, ServiceMethodParams(it.methods))
    }
}