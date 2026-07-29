package com.autoswitch.apk.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.autoswitch.apk.R
import com.autoswitch.apk.databinding.FragmentHomeBinding
import com.autoswitch.apk.utils.NetworkMonitor
import com.autoswitch.apk.utils.PreferencesManager
import com.autoswitch.apk.utils.SimSwitchManager
import com.autoswitch.apk.utils.UpdateManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: PreferencesManager
    private lateinit var simSwitchManager: SimSwitchManager
    private lateinit var updateManager: UpdateManager
    private lateinit var networkMonitor: NetworkMonitor

    private var pulseAnimatorSet: AnimatorSet? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()
        prefs = PreferencesManager(context)
        simSwitchManager = SimSwitchManager(context)
        updateManager = UpdateManager(context)
        networkMonitor = NetworkMonitor.getInstance(context)

        setupPulseAnimation()
        setupListeners()
        checkForAppUpdates()
        setupSimSupportInfo()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkMonitor.networkState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        networkMonitor.startMonitoring()
        networkMonitor.triggerStateCheck()
    }

    override fun onPause() {
        super.onPause()
        // Stop monitoring when not in foreground to save battery if background service is not running
        if (!prefs.isAutoSwitchEnabled) {
            networkMonitor.stopMonitoring()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pulseAnimatorSet?.cancel()
        _binding = null
    }

    private fun setupSimSupportInfo() {
        val hasRoot = simSwitchManager.isRootAvailable()
        if (hasRoot) {
            binding.tvSimSupportValue.text = "Supported (Root)"
            binding.tvSimSupportValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.emerald_accent))
            binding.tvSimSupportWarning.visibility = View.GONE
        } else {
            binding.tvSimSupportValue.text = "Manual (Guided)"
            binding.tvSimSupportValue.setTextColor(ContextCompat.getColor(requireContext(), R.color.amber_warning))
            binding.tvSimSupportWarning.visibility = View.VISIBLE
        }
    }

    private fun checkForAppUpdates() {
        val updateInfo = updateManager.checkForUpdates()
        if (updateInfo.isUpdateAvailable) {
            binding.updateBannerContainer.visibility = View.VISIBLE
            binding.tvUpdateTitle.text = "🚀 New Update Available (v${updateInfo.latestVersionName})"
            binding.tvUpdateNotes.text = updateInfo.releaseNotes
            binding.btnUpdateNow.setOnClickListener {
                updateManager.installLocalApk(updateInfo.localApkFile)
            }
        } else {
            binding.updateBannerContainer.visibility = View.GONE
        }
    }

    private fun setupPulseAnimation() {
        val outerScaleX = ObjectAnimator.ofFloat(binding.pulseRingOuter, View.SCALE_X, 1.0f, 1.35f).apply {
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            duration = 2000
        }
        val outerScaleY = ObjectAnimator.ofFloat(binding.pulseRingOuter, View.SCALE_Y, 1.0f, 1.35f).apply {
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            duration = 2000
        }
        val outerAlpha = ObjectAnimator.ofFloat(binding.pulseRingOuter, View.ALPHA, 0.4f, 0.0f).apply {
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            duration = 2000
        }

        val innerScaleX = ObjectAnimator.ofFloat(binding.pulseRingInner, View.SCALE_X, 1.0f, 1.2f).apply {
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            duration = 2000
            startDelay = 500
        }
        val innerScaleY = ObjectAnimator.ofFloat(binding.pulseRingInner, View.SCALE_Y, 1.0f, 1.2f).apply {
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            duration = 2000
            startDelay = 500
        }
        val innerAlpha = ObjectAnimator.ofFloat(binding.pulseRingInner, View.ALPHA, 0.4f, 0.0f).apply {
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
            duration = 2000
            startDelay = 500
        }

        pulseAnimatorSet = AnimatorSet().apply {
            playTogether(outerScaleX, outerScaleY, outerAlpha, innerScaleX, innerScaleY, innerAlpha)
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun setupListeners() {
        binding.btnOpenSettings.setOnClickListener {
            simSwitchManager.openSimSettings()
        }
    }

    private fun updateUi(state: NetworkMonitor.NetworkState) {
        if (_binding == null) return
        val context = context ?: return

        // 1. Update active connection type labels & styles
        val statusColor: Int
        val ringBgDrawable: Int

        when (state.connectionType) {
            NetworkMonitor.ConnectionType.WIFI -> {
                binding.tvActiveConnectionLabel.text = state.ssid ?: "Wi-Fi Connected"
                binding.ivStatusIcon.setImageResource(R.drawable.ic_wifi)
                statusColor = ContextCompat.getColor(context, R.color.cyan_primary)
                ringBgDrawable = R.drawable.bg_circle_cyan
            }
            NetworkMonitor.ConnectionType.CELLULAR -> {
                val selectedSimText = if (prefs.preferredSimSlot == 0) "SIM 1" else "SIM 2"
                binding.tvActiveConnectionLabel.text = "${state.carrierName ?: "Mobile Data"} ($selectedSimText)"
                binding.ivStatusIcon.setImageResource(R.drawable.ic_cellular)
                statusColor = ContextCompat.getColor(context, R.color.emerald_accent)
                ringBgDrawable = R.drawable.bg_circle_green
            }
            NetworkMonitor.ConnectionType.CHECKING -> {
                binding.tvActiveConnectionLabel.text = "Checking connection..."
                binding.ivStatusIcon.setImageResource(R.drawable.ic_wifi)
                statusColor = ContextCompat.getColor(context, R.color.amber_warning)
                ringBgDrawable = R.drawable.bg_circle_green
            }
            NetworkMonitor.ConnectionType.NONE -> {
                binding.tvActiveConnectionLabel.text = "Offline — no internet"
                binding.ivStatusIcon.setImageResource(R.drawable.ic_offline)
                statusColor = ContextCompat.getColor(context, R.color.red_destructive)
                ringBgDrawable = R.drawable.bg_circle_red
            }
        }

        // Apply colors to icons and rings
        binding.ivStatusIcon.setColorFilter(statusColor)
        binding.pulseRingOuter.setBackgroundResource(ringBgDrawable)
        binding.pulseRingInner.setBackgroundResource(ringBgDrawable)

        // 2. Update internet validation labels
        if (state.isInternetValidated) {
            binding.tvInternetValidationLabel.text = "Internet Access: Available"
            binding.tvInternetValidationLabel.setTextColor(ContextCompat.getColor(context, R.color.emerald_accent))
        } else {
            binding.tvInternetValidationLabel.text = "Internet Access: Unavailable"
            binding.tvInternetValidationLabel.setTextColor(ContextCompat.getColor(context, R.color.red_destructive))
        }

        // 3. Update automation service details
        val preferredSimText = if (prefs.preferredSimSlot == 0) "SIM 1" else "SIM 2"
        binding.tvPreferredSimSlot.text = preferredSimText

        if (prefs.isAutoSwitchEnabled) {
            binding.tvActiveBadge.text = "● Automation Active"
            binding.tvActiveBadge.setTextColor(ContextCompat.getColor(context, R.color.emerald_accent))
            binding.tvActiveBadge.setBackgroundResource(R.drawable.bg_badge_active)
            binding.tvAutomationStatusValue.text = "Active"
            binding.tvAutomationStatusValue.setTextColor(ContextCompat.getColor(context, R.color.emerald_accent))
        } else {
            binding.tvActiveBadge.text = "○ Automation Paused"
            binding.tvActiveBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary_app))
            binding.tvActiveBadge.setBackgroundResource(R.drawable.bg_card_dark)
            binding.tvAutomationStatusValue.text = "Paused"
            binding.tvAutomationStatusValue.setTextColor(ContextCompat.getColor(context, R.color.text_secondary_app))
        }

        // 4. Update change timestamp
        val changeTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(state.lastChangeTime))
        binding.tvLastChangeTime.text = "Last state change: $changeTime"
    }
}
