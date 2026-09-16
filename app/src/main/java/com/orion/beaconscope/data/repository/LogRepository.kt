package com.orion.beaconscope.data.repository

import com.orion.beaconscope.data.model.LogEntry
import com.orion.beaconscope.data.model.LogType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe Repository for managing real-time diagnostic log feeds.
 * Uses bounded circular lists to prevent memory leaks during long-running sessions.
 */
class LogRepository {

    companion object {
        private const val MAX_LOG_SIZE = 100
    }

    private val _logsByBeacon = ConcurrentHashMap<String, CopyOnWriteArrayList<LogEntry>>()
    private val _logsFlow = ConcurrentHashMap<String, MutableStateFlow<List<LogEntry>>>()

    /**
     * Add a log entry for a specific beacon (or "SYSTEM").
     */
    fun addLog(beaconId: String, message: String, type: LogType) {
        val entry = LogEntry(beaconId = beaconId, message = message, type = type)
        
        val list = _logsByBeacon.getOrPut(beaconId) { CopyOnWriteArrayList() }
        list.add(entry)
        
        // Circular buffer constraint
        if (list.size > MAX_LOG_SIZE) {
            list.removeAt(0)
        }

        // Notify subscribers
        val flow = _logsFlow.getOrPut(beaconId) { MutableStateFlow(emptyList()) }
        flow.value = list.toList()
    }

    /**
     * Get a reactive state flow of log entries for a specific beacon or system logs.
     */
    fun getLogs(beaconId: String): StateFlow<List<LogEntry>> {
        return _logsFlow.getOrPut(beaconId) { MutableStateFlow(emptyList()) }.asStateFlow()
    }

    /**
     * Clear all logs for all beacons.
     */
    fun clearAllLogs() {
        _logsByBeacon.clear()
        _logsFlow.forEach { (_, flow) ->
            flow.value = emptyList()
        }
    }
}
