@file:Suppress("DEPRECATION")

package com.aglushkov.wordteacher.shared.general.connectivity

import android.content.Context
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

actual class ConnectivityManager constructor(
    val context: Context
) {

    private var connectivityManager = getConnectivityManager()
    actual var isRegistered = false

    private val stateFlow = MutableStateFlow<Boolean>(false)
    actual val flow: StateFlow<Boolean> = stateFlow

    @Volatile actual var isDeviceOnline = false
        private set

    @Volatile actual var isWifiMode = false
        private set

    private var networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            checkNetworkState()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            checkNetworkState()
        }
    }

    actual fun register() {
        if (!isRegistered) {
            val builder = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
            isRegistered = true
        }
    }

    actual fun unregister() {
        if (isRegistered) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            isRegistered = false
        }
    }

    actual fun checkNetworkState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            updateCapabilities(network)
        } else {
            updateCapabilitiesLegacy(connectivityManager.activeNetworkInfo)
        }

        if (stateFlow.value != isDeviceOnline) {
            stateFlow.value = isDeviceOnline
        }
    }

    private fun updateCapabilities(network: Network?) {
        val capabilities = if (network != null) connectivityManager.getNetworkCapabilities(network) else null
        capabilities?.let {
            isDeviceOnline = true
            isWifiMode = it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } ?: run {
            isDeviceOnline = false
            isWifiMode = false
        }
    }

    private fun updateCapabilitiesLegacy(networkInfo: NetworkInfo?) {
        if (networkInfo?.isConnected == true && networkInfo.isAvailable) {
            isDeviceOnline = true
            val isWifi = networkInfo.type == android.net.ConnectivityManager.TYPE_WIFI
            val isWiMax = networkInfo.type == android.net.ConnectivityManager.TYPE_WIMAX
            isWifiMode = isWifi || isWiMax
        } else {
            isDeviceOnline = false
            isWifiMode = false
        }
    }

    private fun getConnectivityManager() =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
}