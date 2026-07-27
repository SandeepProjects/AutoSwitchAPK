package com.autoswitch.apk.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import com.autoswitch.apk.model.SimInfo
import java.io.DataOutputStream

/**
 * Handles SIM card discovery, default data SIM inspection, and automatic SIM switching.
 */
class SimSwitchManager(private val context: Context) {

    private val subscriptionManager: SubscriptionManager =
        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

    private val telephonyManager: TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private val prefs = PreferencesManager(context)

    /**
     * Retrieves all active SIM card subscriptions on the device.
     */
    @SuppressLint("MissingPermission")
    fun getAvailableSims(): List<SimInfo> {
        val simList = mutableListOf<SimInfo>()
        try {
            val activeSubscriptions: List<SubscriptionInfo>? =
                subscriptionManager.activeSubscriptionInfoList

            val defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()

            activeSubscriptions?.forEach { info ->
                val simInfo = SimInfo(
                    subId = info.subscriptionId,
                    slotIndex = info.simSlotIndex,
                    displayName = info.displayName?.toString() ?: "SIM ${info.simSlotIndex + 1}",
                    carrierName = info.carrierName?.toString() ?: "Unknown Carrier",
                    iccId = info.iccId ?: "",
                    isDataSim = (info.subscriptionId == defaultDataSubId)
                )
                simList.add(simInfo)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied accessing SubscriptionManager", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing SIM cards", e)
        }
        return simList
    }

    /**
     * Executes mobile data switch to the target subscription ID or slot index.
     */
    fun switchToSim(targetSubId: Int, slotIndex: Int): Boolean {
        Log.i(TAG, "Attempting data switch to SubId: $targetSubId (Slot: $slotIndex)")
        prefs.logEvent("Initiating switch to SIM ${slotIndex + 1} (SubId: $targetSubId)...")

        // 1. Try Root Shell Command (Works seamlessly on rooted devices & custom ROMs)
        if (prefs.useRootSwitch && executeRootSimSwitch(targetSubId, slotIndex)) {
            prefs.logEvent("SUCCESS: Switched data SIM to Slot ${slotIndex + 1} via Root Shell.")
            return true
        }

        // 2. Try System Reflection / SubscriptionManager API
        if (executeSystemReflectionSwitch(targetSubId)) {
            prefs.logEvent("SUCCESS: Switched data SIM to SubId $targetSubId via System API.")
            return true
        }

        // 3. Fallback: Prompt user via System Settings intent
        prefs.logEvent("FALLBACK: Launching SIM Settings for manual confirmation.")
        openSimSettings()
        return false
    }

    /**
     * Executes root shell command to switch active data subscription instantly.
     */
    private fun executeRootSimSwitch(subId: Int, slotIndex: Int): Boolean {
        return try {
            val commands = arrayOf(
                "cmd phone data set-data-subscription $subId",
                "settings put global multi_sim_data_call $subId",
                "settings put global user_preferred_data_sub $subId"
            )
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            for (cmd in commands) {
                os.writeBytes("$cmd\n")
            }
            os.writeBytes("exit\n")
            os.flush()
            val exitVal = process.waitFor()
            Log.i(TAG, "Root switch exit code: $exitVal")
            exitVal == 0
        } catch (e: Exception) {
            Log.w(TAG, "Root switch failed or device not rooted: ${e.message}")
            false
        }
    }

    /**
     * Attempts system reflection to invoke SubscriptionManager hidden API `setDefaultDataSubId`.
     */
    private fun executeSystemReflectionSwitch(subId: Int): Boolean {
        return try {
            val method = subscriptionManager.javaClass.getDeclaredMethod(
                "setDefaultDataSubId",
                Int::class.javaPrimitiveType
            )
            method.isAccessible = true
            method.invoke(subscriptionManager, subId)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Reflection switch failed: ${e.message}")
            false
        }
    }

    /**
     * Launches device Network / SIM Settings screen.
     */
    fun openSimSettings() {
        try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    companion object {
        private const val TAG = "SimSwitchManager"
    }
}
