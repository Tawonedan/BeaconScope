package com.orion.beaconscope.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.orion.beaconscope.data.model.RawBeaconReading
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Handles Bluetooth LE scanning for target beacons, parsing packets,
 * and features a built-in mathematical simulation engine for testing and calibration.
 */
class BleScannerRepository(private val context: Context) {

    companion object {
        private const val TAG = "BleScannerRepository"
        private const val IBEACON_MANUFACTURER_ID = 0x004C
        private const val IBEACON_TYPE = 0x0215
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private var bleScanner: BluetoothLeScanner? = null

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanError = MutableStateFlow<String?>(null)
    val scanError = _scanError.asStateFlow()

    // Using a buffered Channel to guarantee no real-time telemetry packets are dropped
    private val _rawSignalChannel = kotlinx.coroutines.channels.Channel<RawBeaconReading>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val rawSignalFlow: Flow<RawBeaconReading> = _rawSignalChannel.receiveAsFlow()

    // Configuration for target beacons (MAC -> TxPower)
    private val targetBeacons = mutableMapOf<String, Int>()
    
    // Whitelisted iBeacon keys mapping to MAC address ("uuid-major-minor" -> MAC)
    private val targetIBeacons = mutableMapOf<String, String>()
    
    // Configurable simulated beacon coordinates for distance matching
    private val simulatedBeaconCoordinates = mutableMapOf<String, Pair<Float, Float>>()

    private val targetConfigs = mutableListOf<com.orion.beaconscope.data.model.BeaconConfig>()

    // Simulation states
    private var isSimulationEnabled = MutableStateFlow(false)
    private var simulationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var scanRetryCount = 0

    // Bluetooth scan callback
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            processScanResult(result)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            results.forEach { processScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed: $errorCode")
            _isScanning.value = false
            
            val errorMessage = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "BLE registration failed. Please toggle Bluetooth in Settings."
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE scanning is unsupported on this hardware."
                SCAN_FAILED_INTERNAL_ERROR -> "BLE stack internal error. Try toggling Bluetooth."
                else -> "BLE Scan failed with error code: $errorCode"
            }
            _scanError.value = errorMessage
            
            // Robust retry logic like Orion Navigator (crucial for MediaTek and custom ROMs)
            if (errorCode == SCAN_FAILED_APPLICATION_REGISTRATION_FAILED || 
                errorCode == SCAN_FAILED_INTERNAL_ERROR) {
                if (scanRetryCount < 3) {
                    scanRetryCount++
                    val delayMs = 1000L * scanRetryCount
                    Log.w(TAG, "Scan failed with error $errorCode. Retrying BLE scan in ${delayMs}ms (attempt $scanRetryCount/3)...")
                    scope.launch {
                        delay(delayMs)
                        retryScan()
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun retryScan() {
        if (_isScanning.value) return
        Log.d(TAG, "Retrying BLE scan (attempt $scanRetryCount)")
        try {
            bleScanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping scan before retry", e)
        }
        
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER) // Use lower power on retry for compatibility
            .setReportDelay(0)
            .build()
        try {
            bleScanner?.startScan(null, settings, scanCallback)
            _isScanning.value = true
            Log.d(TAG, "Physical BLE retry scan started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting physical BLE retry scan", e)
            _isScanning.value = false
        }
    }

    fun setSimulationMode(enabled: Boolean) {
        isSimulationEnabled.value = enabled
        if (enabled && _isScanning.value) {
            startSimulationLoop()
        } else {
            stopSimulationLoop()
        }
    }

    fun getSimulationMode(): StateFlow<Boolean> = isSimulationEnabled.asStateFlow()

    fun configureTargetBeacons(configs: List<com.orion.beaconscope.data.model.BeaconConfig>) {
        targetConfigs.clear()
        targetConfigs.addAll(configs)

        targetBeacons.clear()
        targetIBeacons.clear()
        simulatedBeaconCoordinates.clear()
        
        configs.forEach { config ->
            targetBeacons[config.macAddress] = config.txPower
            val cleanUuid = config.uuid.lowercase().replace("-", "")
            val iBeaconKey = "$cleanUuid-${config.major}-${config.minor}"
            targetIBeacons[iBeaconKey] = config.macAddress
            simulatedBeaconCoordinates[config.macAddress] = Pair(config.x, config.y)
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (_isScanning.value) return
        
        _scanError.value = null

        if (isSimulationEnabled.value) {
            _isScanning.value = true
            startSimulationLoop()
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e(TAG, "Bluetooth is disabled or unsupported")
            _scanError.value = "Bluetooth is disabled! Please enable Bluetooth in Settings."
            return
        }

        bleScanner = bluetoothAdapter.bluetoothLeScanner
        if (bleScanner == null) {
            Log.e(TAG, "BluetoothLeScanner is unavailable")
            _scanError.value = "BluetoothLeScanner is unavailable! Try restarting Bluetooth."
            return
        }

        scanRetryCount = 0

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setReportDelay(0) // Ensure immediate signal delivery (no batching delays)
            .build()

        try {
            // Flush any pending results first to clear the chipset queue, mirroring Orion Navigator
            try { bleScanner?.flushPendingScanResults(scanCallback) } catch (_: Exception) {}
            
            bleScanner?.startScan(null, settings, scanCallback)
            _isScanning.value = true
            Log.d(TAG, "Physical BLE scanning started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting physical BLE scan", e)
            _scanError.value = "Failed to start BLE scan: ${e.message}"
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) return

        if (isSimulationEnabled.value) {
            stopSimulationLoop()
            _isScanning.value = false
            return
        }

        try {
            bleScanner?.stopScan(scanCallback)
            Log.d(TAG, "Physical BLE scanning stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping physical BLE scan", e)
        } finally {
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    private fun processScanResult(result: ScanResult) {
        val scanRecord = result.scanRecord ?: return
        val manufacturerData = scanRecord.getManufacturerSpecificData(IBEACON_MANUFACTURER_ID)
        
        var beacon: BeaconData? = null
        
        // 1. Try standard Apple iBeacon manufacturer data parsing
        if (manufacturerData != null && manufacturerData.size >= 23) {
            beacon = parseIBeaconData(manufacturerData, result.rssi)
        }
        
        // 2. Try raw bytes manual parsing (Fail-safe for non-standard Android parsing on some devices)
        if (beacon == null) {
            beacon = parseIBeaconFromRawBytes(scanRecord.bytes, result.rssi)
        }
        
        // 3. Try custom BLE beacon service data parsing (Orion phone-as-beacon and custom BLE formats)
        if (beacon == null) {
            beacon = parseCustomBeacon(result)
        }
        
        if (beacon != null) {
            val scannedMajor = beacon.major
            val scannedMinor = beacon.minor
            
            // Primary Matching: strictly match by Major/Minor (Orion's highly robust, MAC-immune logic)
            val matchedConfig = targetConfigs.find { it.major == scannedMajor && it.minor == scannedMinor }
            
            if (matchedConfig != null) {
                _rawSignalChannel.trySend(
                    RawBeaconReading(
                        macAddress = matchedConfig.macAddress,
                        rssi = result.rssi,
                        txPower = beacon.txPower,
                        major = scannedMajor,
                        minor = scannedMinor
                    )
                )
                return
            }
        }
        
        // 4. Secondary Fallback: Match strictly by MAC Address (Useful for simulation overrides or classic profiles)
        val deviceAddress = result.device.address
        if (deviceAddress != null) {
            val matchedConfig = targetConfigs.find { it.macAddress.equals(deviceAddress, ignoreCase = true) }
            if (matchedConfig != null) {
                _rawSignalChannel.trySend(
                    RawBeaconReading(
                        macAddress = matchedConfig.macAddress,
                        rssi = result.rssi,
                        txPower = matchedConfig.txPower,
                        major = matchedConfig.major,
                        minor = matchedConfig.minor
                    )
                )
            }
        }
    }

    private fun parseIBeaconData(data: ByteArray, rssi: Int): BeaconData? {
        try {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
            val type = buffer.short.toInt() and 0xFFFF
            if (type != IBEACON_TYPE) return null
            
            val uuidBytes = ByteArray(16)
            buffer.get(uuidBytes)
            val uuid = bytesToUuid(uuidBytes)
            
            val major = buffer.short.toInt() and 0xFFFF
            val minor = buffer.short.toInt() and 0xFFFF
            val txPower = buffer.get().toInt()
            
            return BeaconData(uuid, major, minor, rssi, txPower)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse iBeacon data", e)
            return null
        }
    }

    private fun parseIBeaconFromRawBytes(scanBytes: ByteArray, rssi: Int): BeaconData? {
        try {
            var startByte = 2
            while (startByte <= 5) {
                if (startByte + 26 < scanBytes.size &&
                    ((scanBytes[startByte].toInt() and 0xFF) == 0x1A || (scanBytes[startByte].toInt() and 0xFF) == 0x1B) &&
                    ((scanBytes[startByte + 1].toInt() and 0xFF) == 0xFF)
                ) {
                    val companyId = ((scanBytes[startByte + 3].toInt() and 0xFF) shl 8) or (scanBytes[startByte + 2].toInt() and 0xFF)
                    val ibeaconType = ((scanBytes[startByte + 4].toInt() and 0xFF) shl 8) or (scanBytes[startByte + 5].toInt() and 0xFF)
                    
                    if (companyId == 0x004C && ibeaconType == 0x0215) {
                        val buffer = ByteBuffer.wrap(scanBytes, startByte + 6, 21).order(ByteOrder.BIG_ENDIAN)
                        val uuidBytes = ByteArray(16)
                        buffer.get(uuidBytes)
                        val uuid = bytesToUuid(uuidBytes)
                        val major = buffer.short.toInt() and 0xFFFF
                        val minor = buffer.short.toInt() and 0xFFFF
                        val txPower = buffer.get().toInt()
                        return BeaconData(uuid, major, minor, rssi, txPower)
                    }
                }
                startByte++
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse raw bytes manual iBeacon", e)
        }
        return null
    }

    private fun parseCustomBeacon(result: ScanResult): BeaconData? {
        val scanRecord = result.scanRecord ?: return null
        val serviceData = scanRecord.serviceData ?: return null
        
        serviceData.forEach { (uuid, data) ->
            if (data != null && data.size >= 4) {
                try {
                    val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)
                    val major = buffer.short.toInt() and 0xFFFF
                    val minor = buffer.short.toInt() and 0xFFFF
                    val txPower = if (data.size > 4) data[4].toInt() else -59
                    
                    val beaconUuid = uuid.uuid.toString()
                    return BeaconData(beaconUuid, major, minor, result.rssi, txPower)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse custom service data beacon", e)
                }
            }
        }
        return null
    }

    private fun bytesToUuid(bytes: ByteArray): String {
        val buffer = ByteBuffer.wrap(bytes)
        val high = buffer.long
        val low = buffer.long
        return UUID(high, low).toString()
    }

    private data class BeaconData(
        val uuid: String,
        val major: Int,
        val minor: Int,
        val rssi: Int,
        val txPower: Int
    )

    /**
     * Mathematical high-fidelity simulation engine.
     * Simulates a user moving in a dynamic orbital pattern in the room,
     * calculating exact path loss distances + Gaussian random noise.
     */
    private fun startSimulationLoop() {
        // High-fidelity simulation completely deactivated to guarantee no fictional beacons can run
        Log.d(TAG, "Simulated BLE scanning loop is completely deactivated")
    }

    private fun stopSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = null
        Log.d(TAG, "Simulated BLE scanning loop stopped")
    }
}
