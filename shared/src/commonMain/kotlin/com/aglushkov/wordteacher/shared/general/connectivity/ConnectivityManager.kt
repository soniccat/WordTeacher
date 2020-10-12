package com.aglushkov.wordteacher.shared.general.connectivity

import kotlinx.coroutines.flow.StateFlow
import kotlin.jvm.Volatile

expect class ConnectivityManager {
    val flow: StateFlow<Boolean>

    @Volatile var isDeviceOnline: Boolean
        private set

    @Volatile var isWifiMode: Boolean
        private set

    fun register()
    fun unregister()
    fun checkNetworkState()
}