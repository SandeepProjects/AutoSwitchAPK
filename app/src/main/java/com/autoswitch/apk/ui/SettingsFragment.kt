package com.autoswitch.apk.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.autoswitch.apk.R
import com.autoswitch.apk.databinding.FragmentSettingsBinding
import com.autoswitch.apk.service.WifiMonitorService
import com.autoswitch.apk.utils.PreferencesManager
import com.autoswitch.apk.utils.SimSwitchManager

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: PreferencesManager
    private lateinit var simSwitchManager: SimSwitchManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        prefs = PreferencesManager(context)
        simSwitchManager = SimSwitchManager(context)

        setupThemeSelector()
        setupAutomationSwitch()
        setupSimSelection()
        setupShortcuts()
        loadLogs()
    }

    override fun onResume() {
        super.onResume()
        loadLogs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupThemeSelector() {
        updateThemeButtons(prefs.appTheme)

        binding.btnThemeAuto.setOnClickListener {
            setAppTheme("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        binding.btnThemeLight.setOnClickListener {
            setAppTheme("light", AppCompatDelegate.MODE_NIGHT_NO)
        }
        binding.btnThemeDark.setOnClickListener {
            setAppTheme("dark", AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun setAppTheme(themeKey: String, nightMode: Int) {
        if (prefs.appTheme != themeKey) {
            prefs.appTheme = themeKey
            updateThemeButtons(themeKey)
            AppCompatDelegate.setDefaultNightMode(nightMode)
            requireActivity().recreate()
        }
    }

    private fun updateThemeButtons(themeKey: String) {
        if (_binding == null) return
        val context = requireContext()
        val activeColor = context.getColor(R.color.cyan_primary)
        val inactiveColor = context.getColor(R.color.text_secondary_app)

        binding.btnThemeAuto.setTextColor(if (themeKey == "system") activeColor else inactiveColor)
        binding.btnThemeLight.setTextColor(if (themeKey == "light") activeColor else inactiveColor)
        binding.btnThemeDark.setTextColor(if (themeKey == "dark") activeColor else inactiveColor)
    }

    private fun setupAutomationSwitch() {
        binding.switchAutoSwitch.isChecked = prefs.isAutoSwitchEnabled
        binding.switchAutoSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.isAutoSwitchEnabled = isChecked
            prefs.logEvent("Auto-Switch setting changed: $isChecked")
            loadLogs()

            val context = requireContext()
            val serviceIntent = Intent(context, WifiMonitorService::class.java)
            if (isChecked) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Could not start service: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                try {
                    context.stopService(serviceIntent)
                } catch (e: Exception) {
                    // Ignored
                }
            }
        }
    }

    private fun setupSimSelection() {
        if (prefs.preferredSimSlot == 0) {
            binding.rbSim1.isChecked = true
        } else {
            binding.rbSim2.isChecked = true
        }

        binding.rgSimSelection.setOnCheckedChangeListener { _, checkedId ->
            val slotIndex = if (checkedId == R.id.rbSim1) 0 else 1
            prefs.preferredSimSlot = slotIndex
            prefs.logEvent("Preferred Data SIM updated to SIM ${slotIndex + 1}")
            loadLogs()
        }
    }

    private fun setupShortcuts() {
        binding.rowWirelessSettings.setOnClickListener {
            simSwitchManager.openSimSettings()
        }

        binding.rowBatterySettings.setOnClickListener {
            openIntent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) {
                openIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${requireContext().packageName}"))
            }
        }

        binding.rowDataSettings.setOnClickListener {
            openIntent(Settings.ACTION_DATA_USAGE_SETTINGS) {
                simSwitchManager.openSimSettings()
            }
        }
    }

    private fun openIntent(action: String, fallback: (() -> Unit)? = null) {
        val context = requireContext()
        try {
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            fallback?.invoke() ?: Toast.makeText(context, "Cannot open settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openIntent(action: String, data: Uri) {
        val context = requireContext()
        try {
            val intent = Intent(action, data).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            simSwitchManager.openSimSettings()
        }
    }

    private fun loadLogs() {
        if (_binding == null) return
        binding.tvLogsContent.text = prefs.getLogs()
    }
}
