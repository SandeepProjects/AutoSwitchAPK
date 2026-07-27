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
            Toast.makeText(this, "Permissions granted!", Toast.LENGTH_SHORT).show()
            prefs.logEvent("Permissions granted.")
            loadSimInformation()
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
    }

    private fun setupUI() {
        // Service switch toggle
        binding.switchAutoService.isChecked = prefs.isAutoSwitchEnabled
        binding.switchAutoService.setOnCheckedChangeListener { _, isChecked ->
            prefs.isAutoSwitchEnabled = isChecked
            updateAutoSwitchDisplay()
            if (isChecked) {
                startMonitoringService()
            } else {
                stopMonitoringService()
            }
        }

        // SIM selection radio buttons
        if (prefs.preferredSimSlot == 0) {
            binding.rbSim1.isChecked = true
        } else {
            binding.rbSim2.isChecked = true
        }

        binding.rgSimSelection.setOnCheckedChangeListener { _, checkedId ->
            val slotIndex = if (checkedId == R.id.rbSim1) 0 else 1
            prefs.preferredSimSlot = slotIndex

            val targetSim = availableSims.find { it.slotIndex == slotIndex }
            if (targetSim != null) {
                prefs.preferredSubId = targetSim.subId
            }
            updateAutoSwitchDisplay()
        }

        binding.btnOpenSimSettings.setOnClickListener {
            simSwitchManager.openSimSettings()
        }

        updateAutoSwitchDisplay()
    }

    private fun updateAutoSwitchDisplay() {
        val selectedSimText = if (prefs.preferredSimSlot == 0) "SIM 1" else "SIM 2"
        if (prefs.isAutoSwitchEnabled) {
            binding.tvAutoSwitchValue.text = "On · $selectedSimText"
            binding.tvAutoSwitchValue.setTextColor(ContextCompat.getColor(this, R.color.status_green))
        } else {
            binding.tvAutoSwitchValue.text = "Off"
            binding.tvAutoSwitchValue.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
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
        val sim1 = availableSims.find { it.slotIndex == 0 }
        val sim2 = availableSims.find { it.slotIndex == 1 }

        binding.rbSim1.text = sim1?.let { "SIM 1: ${it.carrierName}" } ?: "SIM 1"
        binding.rbSim2.text = sim2?.let { "SIM 2: ${it.carrierName}" } ?: "SIM 2"
    }

    private fun updateNetworkStatus() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        val isWifiConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellularConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        if (isWifiConnected) {
            binding.tvWifiStatus.text = "Wi-Fi Connected"
            binding.tvWifiSubText.text = "Connected via Wi-Fi"
            binding.tvInternetValue.text = "Available"
            binding.tvInternetValue.setTextColor(ContextCompat.getColor(this, R.color.status_green))
            binding.ivStatusOrb.setImageResource(android.R.drawable.ic_menu_compass)
            binding.ivStatusOrb.setColorFilter(ContextCompat.getColor(this, R.color.status_green))
        } else if (isCellularConnected) {
            val selectedSimText = if (prefs.preferredSimSlot == 0) "SIM 1" else "SIM 2"
            binding.tvWifiStatus.text = "Mobile Data"
            binding.tvWifiSubText.text = "Connected via $selectedSimText"
            binding.tvInternetValue.text = "Available"
            binding.tvInternetValue.setTextColor(ContextCompat.getColor(this, R.color.status_green))
            binding.ivStatusOrb.setImageResource(android.R.drawable.ic_menu_send)
            binding.ivStatusOrb.setColorFilter(ContextCompat.getColor(this, R.color.accent))
        } else {
            binding.tvWifiStatus.text = "No Connection"
            binding.tvWifiSubText.text = "Offline — no internet"
            binding.tvInternetValue.text = "Unavailable"
            binding.tvInternetValue.setTextColor(ContextCompat.getColor(this, R.color.status_red))
            binding.ivStatusOrb.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            binding.ivStatusOrb.setColorFilter(ContextCompat.getColor(this, R.color.status_red))
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
}
