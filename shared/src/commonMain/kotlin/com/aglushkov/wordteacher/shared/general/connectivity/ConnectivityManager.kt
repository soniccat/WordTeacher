package com.aglushkov.wordteacher.shared.general.connectivity

import kotlinx.coroutines.flow.StateFlow
import kotlin.jvm.Volatile

expect class ConnectivityManager {
    var isRegistered: Boolean
    val flow: StateFlow<Boolean>

    var isDeviceOnline: Boolean
        private set

    var isWifiMode: Boolean
        private set

    fun register()
    fun unregister()
    fun checkNetworkState()
}