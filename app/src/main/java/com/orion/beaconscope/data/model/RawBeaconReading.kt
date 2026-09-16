package com.orion.beaconscope.data.model

/**
 * Representation of a raw Bluetooth packet reading.
 */
data class RawBeaconReading(
    val macAddress: String,
    val rssi: Int,
    val txPower: Int,
    val major: Int = 0,
    val minor: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
