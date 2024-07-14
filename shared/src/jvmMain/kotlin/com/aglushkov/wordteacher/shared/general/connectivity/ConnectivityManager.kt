@file:Suppress("DEPRECATION")

package com.aglushkov.wordteacher.shared.general.connectivity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual class ConnectivityManager {

    actual var isRegistered = false

    private val stateFlow = MutableStateFlow<Boolean>(true)
    actual val flow: StateFlow<Boolean> = stateFlow

    @Volatile actual var isDeviceOnline = true
        private set

    @Volatile actual var isWifiMode = true
        private set

    actual fun register() {
        // TODO:
    }

    actual fun unregister() {
        // TODO:
    }

    actual fun checkNetworkState() {
        // TODO:
    }
}