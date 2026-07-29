package com.autoswitch.apk.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkMonitor private constructor(private val context: Context) {

    enum class ConnectionType {
        WIFI, CELLULAR, NONE, CHECKING
    }

    data class NetworkState(
        val connectionType: ConnectionType = ConnectionType.NONE,
        val isInternetValidated: Boolean = false,
        val ssid: String? = null,
        val carrierName: String? = null,
        val lastChangeTime: Long = System.currentTimeMillis()
    )

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkState = MutableStateFlow(NetworkState())
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isRegistered = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.i(TAG, "Callback: Network available")
            triggerStateCheck(ConnectionType.CHECKING)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.i(TAG, "Callback: Network lost")
            triggerStateCheck(ConnectionType.CHECKING)
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            super.onCapabilitiesChanged(network, capabilities)
            Log.i(TAG, "Callback: Network capabilities changed")
            triggerStateCheck()
        }
    }

    @Synchronized
    fun startMonitoring() {
        if (isRegistered) return
        triggerStateCheck()
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
            isRegistered = true
            Log.i(TAG, "Connectivity monitoring started.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    @Synchronized
    fun stopMonitoring() {
        if (!isRegistered) return
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            isRegistered = false
            Log.i(TAG, "Connectivity monitoring stopped.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister network callback", e)
        }
    }

    fun triggerStateCheck(forceType: ConnectionType? = null) {
        mainHandler.postDelayed({
            performStateQuery(forceType)
        }, 100)
    }

    @Synchronized
    private fun performStateQuery(forceType: ConnectionType?) {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        val newState = if (activeNetwork == null || capabilities == null) {
            NetworkState(
                connectionType = ConnectionType.NONE,
                isInternetValidated = false,
                ssid = null,
                carrierName = null,
                lastChangeTime = System.currentTimeMillis()
            )
        } else {
            val isInternetVal = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            val connType = if (forceType == ConnectionType.CHECKING) {
                ConnectionType.CHECKING
            } else {
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
                    else -> ConnectionType.NONE
                }
            }

            val ssidName = if (connType == ConnectionType.WIFI) {
                try {
                    val wifiInfo = capabilities.transportInfo
                    wifiInfo?.toString()?.substringAfter("SSID: ")?.substringBefore(",")?.trim('"')
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }

            val carrierNameString = if (connType == ConnectionType.CELLULAR) {
                getMobileNetworkTypeString()
            } else {
                null
            }

            NetworkState(
                connectionType = connType,
                isInternetValidated = isInternetVal,
                ssid = ssidName,
                carrierName = carrierNameString,
                lastChangeTime = System.currentTimeMillis()
            )
        }

        _networkState.value = newState
        Log.i(TAG, "State updated: connection=${newState.connectionType}, internet=${newState.isInternetValidated}")
    }

    private fun getMobileNetworkTypeString(): String {
        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
            @Suppress("DEPRECATION")
            when (telephonyManager.networkType) {
                android.telephony.TelephonyManager.NETWORK_TYPE_GPRS,
                android.telephony.TelephonyManager.NETWORK_TYPE_EDGE,
                android.telephony.TelephonyManager.NETWORK_TYPE_CDMA,
                android.telephony.TelephonyManager.NETWORK_TYPE_1xRTT,
                android.telephony.TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
                android.telephony.TelephonyManager.NETWORK_TYPE_UMTS,
                android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_0,
                android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_A,
                android.telephony.TelephonyManager.NETWORK_TYPE_HSDPA,
                android.telephony.TelephonyManager.NETWORK_TYPE_HSUPA,
                android.telephony.TelephonyManager.NETWORK_TYPE_HSPA,
                android.telephony.TelephonyManager.NETWORK_TYPE_EVDO_B,
                android.telephony.TelephonyManager.NETWORK_TYPE_EHRPD,
                android.telephony.TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
                android.telephony.TelephonyManager.NETWORK_TYPE_LTE -> "4G"
                android.telephony.TelephonyManager.NETWORK_TYPE_NR -> "5G"
                else -> "Mobile Data"
            }
        } catch (e: Exception) {
            "Mobile Data"
        }
    }

    companion object {
        private const val TAG = "NetworkMonitor"

        @Volatile
        private var INSTANCE: NetworkMonitor? = null

        fun getInstance(context: Context): NetworkMonitor {
            return INSTANCE ?: synchronized(this) {
                val instance = NetworkMonitor(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
