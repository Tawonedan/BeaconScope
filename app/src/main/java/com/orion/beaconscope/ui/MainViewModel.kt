package com.orion.beaconscope.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orion.beaconscope.data.model.BeaconConfig
import com.orion.beaconscope.data.model.LogEntry
import com.orion.beaconscope.data.model.LogType
import com.orion.beaconscope.data.model.RawBeaconReading
import com.orion.beaconscope.data.repository.BleScannerRepository
import com.orion.beaconscope.data.repository.LogRepository
import com.orion.beaconscope.domain.filter.EmaFilter
import com.orion.beaconscope.domain.filter.KalmanFilter
import com.orion.beaconscope.domain.filter.ParticleFilter
import com.orion.beaconscope.domain.filter.SignalFilter
import com.orion.beaconscope.domain.trilateration.MathUtils
import com.orion.beaconscope.domain.trilateration.TrilaterationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

enum class FilterType {
    EMA,
    KALMAN,
    PARTICLE
}

/**
 * Main ViewModel serving as the central orchestration telemetry engine of BeaconScope.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val bleScannerRepository = BleScannerRepository(application.applicationContext)
    private val logRepository = LogRepository()
    private val trilaterationEngine = TrilaterationEngine()

    // 1. Monitored Target Beacon configurations
    val beaconConfigs = listOf(
        BeaconConfig("20:A7:16:5E:CA:35", "01122334-4556-6778-899a-abbccddeeff0", 1, 2, "Kelas 7", 0.0f, 0.0f, -59),
        BeaconConfig("20:A7:16:5E:CA:36", "01122334-4556-6778-899a-abbccddeeff0", 1, 3, "Ruang Guru", 6.0f, 0.0f, -59),
        BeaconConfig("20:A7:16:60:D7:C9", "01122334-4556-6778-899a-abbccddeeff0", 1, 5, "Perpustakaan", 3.0f, 5.2f, -59)
    )

    private val macToConfig = beaconConfigs.associateBy { it.macAddress }

    // 2. Telemetry state parameters
    val isScanning = bleScannerRepository.isScanning
    val isSimulationMode = bleScannerRepository.getSimulationMode()

    private val _selectedFilter = MutableStateFlow(FilterType.KALMAN)
    val selectedFilter: StateFlow<FilterType> = _selectedFilter.asStateFlow()

    // Editable Math Constants
    val emaAlpha = MutableStateFlow(0.2f)
    val kalmanQ = MutableStateFlow(0.05f)
    val kalmanR = MutableStateFlow(3.0f)
    val pathLossExponent = MutableStateFlow(3.0f) // Path loss exponent n
    val rssiThreshold = MutableStateFlow(-95.0f) // Interactive RSSI Threshold Slider

    // 3. Dynamic filter storage per beacon MAC
    private val activeFilters = ConcurrentHashMap<String, SignalFilter>()

    // Telemetry readouts
    private val _rawRssi = MutableStateFlow<Map<String, Int>>(emptyMap())
    val rawRssi = _rawRssi.asStateFlow()

    private val _filteredRssi = MutableStateFlow<Map<String, Float>>(emptyMap())
    val filteredRssi = _filteredRssi.asStateFlow()

    private val _estimatedDistances = MutableStateFlow<Map<String, Float>>(emptyMap())
    val estimatedDistances = _estimatedDistances.asStateFlow()

    private val _estimatedPosition = MutableStateFlow<Pair<Float, Float>?>(null)
    val estimatedPosition = _estimatedPosition.asStateFlow()

    // Telemetry trace history for graphs
    private val _rawHistory = ConcurrentHashMap<String, List<Int>>()
    private val _filteredHistory = ConcurrentHashMap<String, List<Float>>()

    private val _lastSeenTimestamps = ConcurrentHashMap<String, Long>()
    private val _activeBeacons = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val activeBeacons = _activeBeacons.asStateFlow()

    val activeBeaconCount: StateFlow<Int> = _activeBeacons.map { map ->
        map.values.count { it }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val strongestBeacon: StateFlow<StrongestBeaconData?> = combine(_filteredRssi, _activeBeacons) { rssiMap, activeMap ->
        rssiMap.filter { activeMap[it.key] == true }
            .maxByOrNull { it.value }
            ?.let { (macAddress, rssi) ->
                val config = beaconConfigs.find { it.macAddress == macAddress }
                if (config != null) {
                    StrongestBeaconData(
                        name = config.name,
                        macAddress = macAddress,
                        rssi = rssi.toInt(),
                        major = config.major,
                        minor = config.minor
                    )
                } else null
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var scanningCollectionJob: Job? = null
    private var connectionTimeoutJob: Job? = null

    init {
        // Feed scanning repo target info
        updateRepositoryConfiguration()
        initializeFilters()

        // Force simulation mode to false at startup to guarantee physical scanner runs
        bleScannerRepository.setSimulationMode(false)

        // Observe scanning errors for dynamic visual feedback in log terminal
        observeScanErrors()

        // Verify Location services on startup
        checkLocationServices()

        // Connection timeout checker loop (mark beacon lost if no frames for 3.5s)
        startTimeoutChecker()
    }

    fun toggleScan() {
        viewModelScope.launch {
            if (isScanning.value) {
                bleScannerRepository.stopScan()
                scanningCollectionJob?.cancel()
                logRepository.addLog("SYSTEM", "BLE Telemetry scan stopped manually", LogType.INFO)
            } else {
                logRepository.clearAllLogs()
                _rawHistory.clear()
                _filteredHistory.clear()
                _rawRssi.value = emptyMap()
                _filteredRssi.value = emptyMap()
                _estimatedDistances.value = emptyMap()
                _estimatedPosition.value = null
                initializeFilters()

                logRepository.addLog("SYSTEM", "Starting BLE Telemetry scan...", LogType.INFO)
                checkLocationServices() // Explicit check before scanning starts
                bleScannerRepository.startScan()
                startCollectingScanReadings()
            }
        }
    }

    fun setSimulationMode(enabled: Boolean) {
        viewModelScope.launch {
            val wasScanning = isScanning.value
            if (wasScanning) {
                bleScannerRepository.stopScan()
                scanningCollectionJob?.cancel()
            }

            bleScannerRepository.setSimulationMode(enabled)
            updateRepositoryConfiguration()

            logRepository.addLog(
                "SYSTEM",
                "Mode switched to: ${if (enabled) "SIMULATOR" else "PHYSICAL SCANNER"}",
                LogType.INFO
            )

            if (wasScanning) {
                bleScannerRepository.startScan()
                startCollectingScanReadings()
            }
        }
    }

    fun setFilterType(type: FilterType) {
        _selectedFilter.value = type
        initializeFilters()
        logRepository.addLog("SYSTEM", "Switched global filter algorithm to: $type", LogType.INFO)
    }

    fun getLogs(beaconId: String) = logRepository.getLogs(beaconId)

    fun getRawHistory(mac: String): List<Int> = _rawHistory[mac] ?: emptyList()
    fun getFilteredHistory(mac: String): List<Float> = _filteredHistory[mac] ?: emptyList()

    private fun updateRepositoryConfiguration() {
        bleScannerRepository.configureTargetBeacons(beaconConfigs)
    }

    private fun initializeFilters() {
        activeFilters.clear()
        beaconConfigs.forEach { beacon ->
            val filter = when (_selectedFilter.value) {
                FilterType.EMA -> EmaFilter(alpha = emaAlpha.value)
                FilterType.KALMAN -> KalmanFilter(q = kalmanQ.value, r = kalmanR.value)
                FilterType.PARTICLE -> ParticleFilter(numParticles = 80, processNoise = 0.5f, measurementNoise = 4.0f)
            }
            activeFilters[beacon.macAddress] = filter
        }
    }

    private fun startCollectingScanReadings() {
        scanningCollectionJob?.cancel()
        scanningCollectionJob = viewModelScope.launch(Dispatchers.Default) {
            bleScannerRepository.rawSignalFlow.collect { reading ->
                processIncomingReading(reading)
            }
        }
    }

    private fun processIncomingReading(reading: RawBeaconReading) {
        val config = macToConfig[reading.macAddress] ?: return
        val filter = activeFilters[reading.macAddress] ?: return

        val rawVal = reading.rssi
        if (rawVal < rssiThreshold.value) {
            // Signal is weaker than dynamic detection threshold! Drop telemetry reading.
            return
        }

        val filteredVal = filter.filter(rawVal.toFloat())

        // 1. Update trace histories for plotting
        val rawList = (_rawHistory[reading.macAddress] ?: emptyList()).toMutableList()
        rawList.add(rawVal)
        if (rawList.size > 50) rawList.removeAt(0)
        _rawHistory[reading.macAddress] = rawList

        val filteredList = (_filteredHistory[reading.macAddress] ?: emptyList()).toMutableList()
        filteredList.add(filteredVal)
        if (filteredList.size > 50) filteredList.removeAt(0)
        _filteredHistory[reading.macAddress] = filteredList

        // 2. Calibrate distance using Path Loss Equation
        val distance = MathUtils.calculateDistance(
            rssi = filteredVal,
            txPower = config.txPower,
            n = pathLossExponent.value
        )

        // 3. Keep track of live parameters
        _lastSeenTimestamps[reading.macAddress] = System.currentTimeMillis()
        _rawRssi.value = _rawRssi.value.toMutableMap().apply { put(reading.macAddress, rawVal) }
        _filteredRssi.value = _filteredRssi.value.toMutableMap().apply { put(reading.macAddress, filteredVal) }
        _estimatedDistances.value = _estimatedDistances.value.toMutableMap().apply { put(reading.macAddress, distance) }
        _activeBeacons.value = _activeBeacons.value.toMutableMap().apply { put(reading.macAddress, true) }

        // 4. Log detailed diagnostics to individual logs
        logRepository.addLog(reading.macAddress, "Raw RSSI: $rawVal dBm", LogType.RSSI)
        logRepository.addLog(
            reading.macAddress,
            String.format("Filtered: %.2f dBm (${_selectedFilter.value})", filteredVal),
            LogType.FILTER
        )
        logRepository.addLog(
            reading.macAddress,
            String.format("Est. Distance: %.3fm", distance),
            LogType.INFO
        )

        // Log detailed diagnostics to system logs
        logRepository.addLog(
            "SYSTEM",
            "Scanned: ${config.name} (MAC=${reading.macAddress}, ScannedMaj=${reading.major}, ScannedMin=${reading.minor}) RSSI=${rawVal}dBm",
            LogType.INFO
        )

        // 5. Execute 2D trilateration computations
        triggerTrilaterationSolver()
    }

    private fun triggerTrilaterationSolver() {
        val dMap = _estimatedDistances.value
        val b1 = beaconConfigs[0]
        val b2 = beaconConfigs[1]
        val b3 = beaconConfigs[2]

        val d1 = dMap[b1.macAddress]
        val d2 = dMap[b2.macAddress]
        val d3 = dMap[b3.macAddress]

        // Solve trilateration only if we have distance telemetry for all 3 beacons
        if (d1 != null && d2 != null && d3 != null) {
            val userPosition = trilaterationEngine.calculatePosition2D(
                x1 = b1.x, y1 = b1.y, d1 = d1,
                x2 = b2.x, y2 = b2.y, d2 = d2,
                x3 = b3.x, y3 = b3.y, d3 = d3
            )

            if (userPosition != null) {
                _estimatedPosition.value = userPosition
                logRepository.addLog(
                    "SYSTEM",
                    String.format("Trilaterated Position: (X: %.2fm, Y: %.2fm)", userPosition.first, userPosition.second),
                    LogType.TRILATERATION
                )
            }
        }
    }

    private fun startTimeoutChecker() {
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(1000L)
                val now = System.currentTimeMillis()
                var updated = false
                val currentActives = _activeBeacons.value.toMutableMap()

                _lastSeenTimestamps.forEach { (mac, time) ->
                    if (now - time > 3500L && currentActives[mac] == true) {
                        currentActives[mac] = false
                        updated = true
                        
                        // Clear stale metrics so the circles disappear from the radar and numbers clear from the UI!
                        _rawRssi.value = _rawRssi.value.toMutableMap().apply { remove(mac) }
                        _filteredRssi.value = _filteredRssi.value.toMutableMap().apply { remove(mac) }
                        _estimatedDistances.value = _estimatedDistances.value.toMutableMap().apply { remove(mac) }
                        
                        logRepository.addLog(mac, "Beacon connection lost (timeout 3.5s)", LogType.ERROR)
                        logRepository.addLog("SYSTEM", "Warning: Lost signal trace for beacon $mac", LogType.ERROR)
                    }
                }

                if (updated) {
                    _activeBeacons.value = currentActives
                }
            }
        }
    }
    private fun checkLocationServices() {
        val context = getApplication<Application>().applicationContext
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? android.location.LocationManager
        val isGpsEnabled = locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true
        val isNetworkEnabled = locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
        if (!isGpsEnabled && !isNetworkEnabled) {
            logRepository.addLog(
                "SYSTEM",
                "⚠️ ERROR: GPS Location Services are globally disabled on your phone! Android BLE scan requires GPS to be active.",
                LogType.ERROR
            )
        }
    }

    private fun observeScanErrors() {
        viewModelScope.launch {
            bleScannerRepository.scanError.collect { error ->
                if (error != null) {
                    logRepository.addLog("SYSTEM", "❌ ERROR: $error", LogType.ERROR)
                    _activeBeacons.value = _activeBeacons.value.toMutableMap().apply {
                        keys.forEach { put(it, false) }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        bleScannerRepository.stopScan()
        scanningCollectionJob?.cancel()
        connectionTimeoutJob?.cancel()
    }
}

data class StrongestBeaconData(
    val name: String,
    val macAddress: String,
    val rssi: Int,
    val major: Int,
    val minor: Int
)
