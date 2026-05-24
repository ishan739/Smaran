package com.example.smaran

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class NetworkStatus {
    object Available  : NetworkStatus()
    object Unavailable: NetworkStatus()
    object LowSpeed   : NetworkStatus()
    object Unstable   : NetworkStatus()
}

class NetworkMonitor(context: Context) {

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _status = MutableStateFlow(currentStatus())
    val status: StateFlow<NetworkStatus> = _status.asStateFlow()

    private var lastKbps = 0
    private val recentSwings = ArrayDeque<Long>()

    private val callback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            _status.value = currentStatus()
        }

        override fun onLost(network: Network) {
            lastKbps = 0
            recentSwings.clear()
            _status.value = NetworkStatus.Unavailable
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                _status.value = NetworkStatus.Unavailable
                return
            }

            val kbps = caps.linkDownstreamBandwidthKbps
            val now  = System.currentTimeMillis()

            // A swing is a >50% change in bandwidth — sign of instability
            if (lastKbps > 0 && abs(kbps - lastKbps) > lastKbps / 2) {
                recentSwings.addLast(now)
            }
            lastKbps = kbps

            // Forget swings older than 15 s
            while (recentSwings.isNotEmpty() && now - recentSwings.first() > 15_000) {
                recentSwings.removeFirst()
            }

            _status.value = when {
                kbps in 1..149          -> NetworkStatus.LowSpeed
                recentSwings.size >= 3  -> NetworkStatus.Unstable
                else                    -> NetworkStatus.Available
            }
        }

        override fun onUnavailable() {
            _status.value = NetworkStatus.Unavailable
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)
    }

    private fun currentStatus(): NetworkStatus {
        val network = cm.activeNetwork ?: return NetworkStatus.Unavailable
        val caps    = cm.getNetworkCapabilities(network) ?: return NetworkStatus.Unavailable
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
            !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            return NetworkStatus.Unavailable
        }
        val kbps = caps.linkDownstreamBandwidthKbps
        return if (kbps in 1..149) NetworkStatus.LowSpeed else NetworkStatus.Available
    }

    fun unregister() {
        runCatching { cm.unregisterNetworkCallback(callback) }
    }
}