package com.autoswitch.apk.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.autoswitch.apk.MainActivity
import com.autoswitch.apk.R
import com.autoswitch.apk.utils.PreferencesManager
import com.autoswitch.apk.utils.SimSwitchManager

/**
 * Foreground Service that continuously monitors Wi-Fi network availability.
 * When Wi-Fi drops, it automatically triggers mobile data switch to the user's preferred SIM.
 */
class WifiMonitorService : Service() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var simSwitchManager: SimSwitchManager
    private lateinit var prefs: PreferencesManager

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isWifiConnected = false

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "WifiMonitorService created.")
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        simSwitchManager = SimSwitchManager(this)
        prefs = PreferencesManager(this)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Monitoring Wi-Fi network state..."))

        registerWifiNetworkCallback()
    }

    private fun registerWifiNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.i(TAG, "Wi-Fi Network Connected.")
                isWifiConnected = true
                prefs.logEvent("Wi-Fi Connected.")
                updateNotification("Wi-Fi Connected. Monitoring active.")
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.w(TAG, "Wi-Fi Network Lost / Disconnected!")
                isWifiConnected = false
                prefs.logEvent("ALERT: Wi-Fi network disconnected!")

                if (prefs.isAutoSwitchEnabled) {
                    val preferredSlot = prefs.preferredSimSlot
                    val preferredSubId = prefs.preferredSubId
                    prefs.logEvent("Triggering auto-switch to preferred SIM ${preferredSlot + 1}...")

                    simSwitchManager.switchToSim(preferredSubId, preferredSlot)
                    updateNotification("Wi-Fi Lost! Switched Mobile Data to SIM ${preferredSlot + 1}.")
                } else {
                    updateNotification("Wi-Fi Lost. Auto-Switch is disabled.")
                }
            }
        }

        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "WifiMonitorService onStartCommand.")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "WifiMonitorService destroyed.")
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister network callback", e)
            }
        }
        prefs.logEvent("Auto-Switch Service Stopped.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AutoSwitch Wi-Fi & SIM Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors Wi-Fi network to auto-switch SIM mobile data"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AutoSwitch APK Service")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    companion object {
        private const val TAG = "WifiMonitorService"
        private const val CHANNEL_ID = "autoswitch_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
