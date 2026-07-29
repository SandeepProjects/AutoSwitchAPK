package com.autoswitch.apk.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages persistent configuration settings for SIM selection, auto-switch preferences, and theme mode.
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var isAutoSwitchEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SWITCH_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SWITCH_ENABLED, value).apply()

    var preferredSubId: Int
        get() = prefs.getInt(KEY_PREFERRED_SUB_ID, -1)
        set(value) = prefs.edit().putInt(KEY_PREFERRED_SUB_ID, value).apply()

    var preferredSimSlot: Int
        get() = prefs.getInt(KEY_PREFERRED_SIM_SLOT, 0) // Default to Slot 0 (SIM 1)
        set(value) = prefs.edit().putInt(KEY_PREFERRED_SIM_SLOT, value).apply()

    var useRootSwitch: Boolean
        get() = prefs.getBoolean(KEY_USE_ROOT_SWITCH, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_ROOT_SWITCH, value).apply()

    var appTheme: String
        get() = prefs.getString(KEY_APP_THEME, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_APP_THEME, value).apply()

    fun logEvent(event: String) {
        val currentLogs = prefs.getString(KEY_EVENT_LOGS, "") ?: ""
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val newLog = "[$timestamp] $event\n" + currentLogs.take(2000)
        prefs.edit().putString(KEY_EVENT_LOGS, newLog).apply()
    }

    fun getLogs(): String {
        return prefs.getString(KEY_EVENT_LOGS, "[App Ready] AutoSwitch initialized.") ?: ""
    }

    companion object {
        private const val PREF_NAME = "autoswitch_prefs"
        private const val KEY_AUTO_SWITCH_ENABLED = "auto_switch_enabled"
        private const val KEY_PREFERRED_SUB_ID = "preferred_sub_id"
        private const val KEY_PREFERRED_SIM_SLOT = "preferred_sim_slot"
        private const val KEY_USE_ROOT_SWITCH = "use_root_switch"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_EVENT_LOGS = "event_logs"
    }
}
