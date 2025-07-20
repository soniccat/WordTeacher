package com.aglushkov.wordteacher.shared.repository.deviceid

import com.aglushkov.wordteacher.shared.general.settings.SettingStore
import com.benasher44.uuid.uuid4
import com.russhwolf.settings.coroutines.FlowSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

interface DeviceIdProvider {
    fun generateDeviceId(): String
}

class DeviceIdRepository(
    private val settings: SettingStore,
    private val deviceIdProvider: DeviceIdProvider = object : DeviceIdProvider {
        override fun generateDeviceId(): String {
            return uuid4().toString()
        }
    },
) {
    private var loadedDeviceId = MutableStateFlow<String?>(null)
    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        mainScope.launch(Dispatchers.Default) {
            loadDeviceIdOrGenerateIfNeeded()
        }
    }

    fun deviceId(): String {
        loadedDeviceId.value?.let {
            return  it
        }

        return runBlocking {
            loadedDeviceId.first { it != null }!!
        }
    }

    private suspend fun loadDeviceIdOrGenerateIfNeeded(): String {
        val deviceId = withContext(Dispatchers.Default) {
            val loadedDeviceId = settings.string(DEVICE_ID) ?: ""
            if (loadedDeviceId.isEmpty()) {
                val generatedDeviceId = deviceIdProvider.generateDeviceId()
                settings[DEVICE_ID] = generatedDeviceId
                generatedDeviceId
            } else {
                loadedDeviceId
            }
        }
        loadedDeviceId.compareAndSet(null, deviceId)
        return deviceId
    }
}

private const val DEVICE_ID = "deviceId"
