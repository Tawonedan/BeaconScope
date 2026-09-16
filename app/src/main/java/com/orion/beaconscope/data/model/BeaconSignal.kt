package com.orion.beaconscope.data.model

/**
 * Live signal reading representation for a BLE Beacon.
 */
data class BeaconSignal(
    val macAddress: String,
    val beaconName: String,
    val rawRssi: Int,
    val filteredRssi: Float,
    val estimatedDistance: Float,
    val timestamp: Long = System.currentTimeMillis()
)
