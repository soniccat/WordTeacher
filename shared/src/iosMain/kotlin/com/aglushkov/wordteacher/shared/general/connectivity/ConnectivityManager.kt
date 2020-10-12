package com.aglushkov.wordteacher.shared.general.connectivity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual class ConnectivityManager {

    private val stateFlow = MutableStateFlow<Boolean>(false)
    actual val flow: StateFlow<Boolean> = stateFlow

    actual var isDeviceOnline = false
        private set

    actual var isWifiMode = false
        private set


    actual fun register() {
        registerNetworkCallback()
    }

    actual fun unregister() {
        //
    }

    private fun registerNetworkCallback() {
        //
    }

    actual fun checkNetworkState() {
        //
    }

}