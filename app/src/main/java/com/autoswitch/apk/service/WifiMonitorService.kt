package com.autoswitch.apk.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.autoswitch.apk.MainActivity
import com.autoswitch.apk.R
import com.autoswitch.apk.utils.NetworkMonitor
import com.autoswitch.apk.utils.PreferencesManager
import com.autoswitch.apk.utils.SimSwitchManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground Service that continuously monitors network connectivity.
 * When Wi-Fi drops, it automatically triggers mobile data switch or guided alerts.
 */
class WifiMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var simSwitchManager: SimSwitchManager
    private lateinit var prefs: PreferencesManager

    private var isWifiConnected = false

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "WifiMonitorService created.")
        simSwitchManager = SimSwitchManager(this)
        prefs = PreferencesManager(this)
        networkMonitor = NetworkMonitor.getInstance(this)

        createNotificationChannel()
        try {
            startForeground(NOTIFICATION_ID, buildNotification("Monitoring network connectivity..."))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to startForeground", e)
        }

        networkMonitor.startMonitoring()

        serviceScope.launch {
            networkMonitor.networkState.collect { state ->
                handleNetworkStateChange(state)
            }
        }
    }

    private fun handleNetworkStateChange(state: NetworkMonitor.NetworkState) {
        val currentlyWifi = (state.connectionType == NetworkMonitor.ConnectionType.WIFI)

        if (isWifiConnected && !currentlyWifi) {
            Log.w(TAG, "Wi-Fi connection lost detected.")
            prefs.logEvent("ALERT: Wi-Fi network disconnected!")

            if (prefs.isAutoSwitchEnabled) {
                val preferredSlot = prefs.preferredSimSlot
                val preferredSubId = prefs.preferredSubId
                prefs.logEvent("Triggering auto-switch to preferred SIM ${preferredSlot + 1}...")

                val isSuccess = simSwitchManager.switchToSim(preferredSubId, preferredSlot)
                if (isSuccess) {
                    updateNotification("Wi-Fi Lost! Auto-switched mobile data to SIM ${preferredSlot + 1}.")
                } else {
                    updateNotification("Wi-Fi Lost! Guided action: Please select SIM ${preferredSlot + 1}.")
                }
            } else {
                updateNotification("Wi-Fi Lost. Automation is paused.")
            }
        } else if (!isWifiConnected && currentlyWifi) {
            Log.i(TAG, "Wi-Fi connection restored.")
            prefs.logEvent("Wi-Fi Connected.")
            updateNotification("Wi-Fi Connected. Monitoring active.")
        }

        isWifiConnected = currentlyWifi
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "WifiMonitorService onStartCommand.")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "WifiMonitorService destroyed.")
        serviceScope.cancel()
        networkMonitor.stopMonitoring()
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
            .setSmallIcon(R.drawable.ic_nav_home)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification(contentText))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update notification", e)
        }
    }

    companion object {
        private const val TAG = "WifiMonitorService"
        private const val CHANNEL_ID = "autoswitch_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
