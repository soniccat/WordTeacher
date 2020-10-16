package com.aglushkov.wordteacher.shared.general.connectivity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import cocoapods.Reachability.*
import platform.Foundation.NSNotificationCenter
import kotlin.native.concurrent.ensureNeverFrozen
import kotlin.native.concurrent.freeze

actual class ConnectivityManager {
    private val reachability = Reachability.reachabilityForInternetConnection()!!
    actual var isRegistered = false

    private val stateFlow = MutableStateFlow(false)
    actual val flow: StateFlow<Boolean> = stateFlow

    actual var isDeviceOnline = false
        private set

    actual var isWifiMode = false
        private set

    init {
        ensureNeverFrozen()
        reachability.freeze()

        NSNotificationCenter.defaultCenter.addObserverForName(
            name = kReachabilityChangedNotification,
            `object` = null,
            queue = null,
            usingBlock = {
                checkNetworkState()
            })
    }

    actual fun register() {
        if (!isRegistered) {
            reachability.startNotifier()
            isRegistered = true
        }
    }

    actual fun unregister() {
        if (isRegistered) {
            reachability.stopNotifier()
            isRegistered = false
        }
    }

    actual fun checkNetworkState() {
        this.isDeviceOnline = reachability.isReachable()
        this.isWifiMode = reachability.isReachableViaWiFi()

        //NSLog("isOnline %d", reachability.isReachable())
        if (stateFlow.value != isDeviceOnline) {
            stateFlow.value = isDeviceOnline
        }
    }
}