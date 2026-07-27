package com.autoswitch.apk.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.autoswitch.apk.service.WifiMonitorService
import com.autoswitch.apk.utils.PreferencesManager

/**
 * Receiver that auto-starts the WifiMonitorService when device boots up.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "BootReceiver received action: $action")

        if (Intent.ACTION_BOOT_COMPLETED == action || Intent.ACTION_MY_PACKAGE_REPLACED == action) {
            val prefs = PreferencesManager(context)
            if (prefs.isAutoSwitchEnabled) {
                Log.i(TAG, "Auto-Switch is enabled. Starting WifiMonitorService on boot.")
                prefs.logEvent("Boot completed: Starting AutoSwitch Service.")

                val serviceIntent = Intent(context, WifiMonitorService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
