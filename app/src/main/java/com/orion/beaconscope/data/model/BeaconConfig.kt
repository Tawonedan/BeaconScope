package com.orion.beaconscope.data.model

/**
 * Configuration model for a tracked BLE Beacon.
 */
data class BeaconConfig(
    val macAddress: String,
    val uuid: String,
    val major: Int,
    val minor: Int,
    val name: String,
    val x: Float, // X coordinate in meters
    val y: Float, // Y coordinate in meters
    val txPower: Int // Calibrated RSSI at 1 meter
)
