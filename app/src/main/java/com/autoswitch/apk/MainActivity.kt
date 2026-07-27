package com.autoswitch.apk

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.autoswitch.apk.databinding.ActivityMainBinding
import com.autoswitch.apk.model.SimInfo
import com.autoswitch.apk.service.WifiMonitorService
import com.autoswitch.apk.utils.PreferencesManager
import com.autoswitch.apk.utils.SimSwitchManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var simSwitchManager: SimSwitchManager

    private var availableSims: List<SimInfo> = emptyList()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show()
            prefs.logEvent("Permissions granted by user.")
            loadSimInformation()
        } else {
            Toast.makeText(this, "Some permissions were denied. SIM detection may be limited.", Toast.LENGTH_LONG).show()
            prefs.logEvent("Warning: Some permissions denied.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)
        simSwitchManager = SimSwitchManager(this)

        setupUI()
        checkPermissions()
        loadSimInformation()
        updateNetworkStatus()
    }

    override fun onResume() {
        super.onResume()
        updateNetworkStatus()
        refreshLogs()
    }

    private fun setupUI() {
        // Restore saved Auto-Switch toggle state
        binding.switchAutoService.isChecked = prefs.isAutoSwitchEnabled
        binding.switchAutoService.setOnCheckedChangeListener { _, isChecked ->
            prefs.isAutoSwitchEnabled = isChecked
            if (isChecked) {
                startMonitoringService()
                prefs.logEvent("Auto-Switch Service Enabled.")
            } else {
                stopMonitoringService()
                prefs.logEvent("Auto-Switch Service Disabled.")
            }
            refreshLogs()
        }

        // Restore SIM Selection
        if (prefs.preferredSimSlot == 0) {
            binding.rbSim1.isChecked = true
        } else {
            binding.rbSim2.isChecked = true
        }

        binding.rgSimSelection.setOnCheckedChangeListener { _, checkedId ->
            val slotIndex = if (checkedId == R.id.rbSim1) 0 else 1
            prefs.preferredSimSlot = slotIndex

            // Update subId if available
            val targetSim = availableSims.find { it.slotIndex == slotIndex }
            if (targetSim != null) {
                prefs.preferredSubId = targetSim.subId
            }
            prefs.logEvent("Preferred fallback set to SIM ${slotIndex + 1}.")
            refreshLogs()
        }

        // Action Buttons
        binding.btnGrantPermissions.setOnClickListener {
            checkPermissions()
        }

        binding.btnTestSwitch.setOnClickListener {
            val selectedSlot = prefs.preferredSimSlot
            val selectedSubId = prefs.preferredSubId
            Toast.makeText(this, "Testing SIM Switch to SIM ${selectedSlot + 1}...", Toast.LENGTH_SHORT).show()
            simSwitchManager.switchToSim(selectedSubId, selectedSlot)
            refreshLogs()
        }

        binding.btnOpenSimSettings.setOnClickListener {
            simSwitchManager.openSimSettings()
        }
    }

    private fun checkPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun loadSimInformation() {
        availableSims = simSwitchManager.getAvailableSims()
        if (availableSims.isNotEmpty()) {
            val sim1 = availableSims.find { it.slotIndex == 0 }
            val sim2 = availableSims.find { it.slotIndex == 1 }

            binding.rbSim1.text = sim1?.let { "SIM 1: ${it.carrierName}" } ?: "SIM 1 (Not Inserted)"
            binding.rbSim2.text = sim2?.let { "SIM 2: ${it.carrierName}" } ?: "SIM 2 (Not Inserted)"

            val activeDataSim = availableSims.find { it.isDataSim }
            if (activeDataSim != null) {
                binding.tvActiveSimStatus.text = "Active Mobile Data SIM: Slot ${activeDataSim.slotIndex + 1} (${activeDataSim.carrierName})"
            } else {
                binding.tvActiveSimStatus.text = "Active Mobile Data SIM: Slot ${prefs.preferredSimSlot + 1}"
            }
        } else {
            binding.tvActiveSimStatus.text = "Active Mobile Data SIM: Slot ${prefs.preferredSimSlot + 1} (Grant Read Phone Permission)"
        }
    }

    private fun updateNetworkStatus() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        val isWifiConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        if (isWifiConnected) {
            binding.tvWifiStatus.text = "Wi-Fi: Connected"
            binding.tvWifiBadge.text = "CONNECTED"
            binding.tvWifiBadge.setTextColor(ContextCompat.getColor(this, R.color.status_green))
            binding.tvWifiBadge.setBackgroundColor(0x3310B981)
        } else {
            binding.tvWifiStatus.text = "Wi-Fi: Disconnected"
            binding.tvWifiBadge.text = "DISCONNECTED"
            binding.tvWifiBadge.setTextColor(ContextCompat.getColor(this, R.color.status_red))
            binding.tvWifiBadge.setBackgroundColor(0x33EF4444)
        }
    }

    private fun startMonitoringService() {
        val serviceIntent = Intent(this, WifiMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopMonitoringService() {
        val serviceIntent = Intent(this, WifiMonitorService::class.java)
        stopService(serviceIntent)
    }

    private fun refreshLogs() {
        binding.tvLogs.text = prefs.getLogs()
    }
}
