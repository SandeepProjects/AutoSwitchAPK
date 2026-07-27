package com.autoswitch.apk.model

/**
 * Data class representing a SIM card subscription on the device.
 */
data class SimInfo(
    val subId: Int,
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String,
    val iccId: String = "",
    val isDataSim: Boolean = false
)
