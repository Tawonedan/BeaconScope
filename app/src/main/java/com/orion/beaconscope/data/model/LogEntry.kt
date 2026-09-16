package com.orion.beaconscope.data.model

enum class LogType {
    INFO,
    RSSI,
    FILTER,
    TRILATERATION,
    ERROR
}

/**
 * Diagnostic log item shown in the expandable log terminals.
 */
data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val beaconId: String, // MAC Address or "SYSTEM"
    val message: String,
    val type: LogType
)
