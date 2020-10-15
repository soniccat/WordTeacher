package com.aglushkov.wordteacher.shared.general.connectivity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import cocoapods.Reachability.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual class ConnectivityManager {
    private val reachability = Reachability.reachabilityForInternetConnection()!!

    private val stateFlow = MutableStateFlow<Boolean>(false)
    actual val flow: StateFlow<Boolean> = stateFlow

    actual var isDeviceOnline = false
        private set

    actual var isWifiMode = false
        private set

    init {
        reachability.reachableBlock = {
            val isReachable = it!!.isReachable()
            val isOverWifi = it!!.isReachableViaWiFi()

            dispatch_async(dispatch_get_main_queue()) {
                updateNetworkState(isReachable, isOverWifi)
            }
        }

        reachability.unreachableBlock = {
            val isReachable = it!!.isReachable()
            val isOverWifi = it!!.isReachableViaWiFi()

            dispatch_async(dispatch_get_main_queue()) {
                updateNetworkState(isReachable, isOverWifi)
            }
        }
    }

    actual fun register() {
        reachability.startNotifier()
    }

    actual fun unregister() {
        reachability.stopNotifier()
    }

    actual fun checkNetworkState() {
        updateNetworkState(reachability.isReachable(), reachability.isReachableViaWiFi())
    }

    private fun updateNetworkState(isReachable: Boolean, isOverWifi: Boolean) {
        this.isDeviceOnline = isReachable
        this.isWifiMode = isOverWifi

        if (stateFlow.value != isDeviceOnline) {
            stateFlow.value = isDeviceOnline
        }
    }
}