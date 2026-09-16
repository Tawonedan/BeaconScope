package com.orion.beaconscope.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.beaconscope.ui.components.BeaconCard
import com.orion.beaconscope.ui.components.LogTerminal
import com.orion.beaconscope.ui.components.RadarPositionView
import com.orion.beaconscope.ui.theme.*

/**
 * Root Composable composing the Premium Cyber-Dark Dashboard of BeaconScope.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val isScanning by viewModel.isScanning.collectAsState()
    val isSimulationMode by viewModel.isSimulationMode.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    val rawRssiMap by viewModel.rawRssi.collectAsState()
    val filteredRssiMap by viewModel.filteredRssi.collectAsState()
    val estimatedDistanceMap by viewModel.estimatedDistances.collectAsState()
    val estimatedPosition by viewModel.estimatedPosition.collectAsState()
    val activeBeaconsMap by viewModel.activeBeacons.collectAsState()

    val systemLogs by viewModel.getLogs("SYSTEM").collectAsState(initial = emptyList())

    // UI state for parameter adjustments panel
    var showParamsPanel by remember { mutableStateOf(false) }

    // Scroll state for dashboard
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "BEACON",
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "SCOPE",
                            color = CyberBlue,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 1.sp
                        )
                    }
                },
                actions = {
                    // Scanning state toggle switch
                    Text(
                        text = if (isScanning) "TELEMETRY ON" else "TELEMETRY OFF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isScanning) NeonGreen else TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = isScanning,
                        onCheckedChange = { viewModel.toggleScan() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonGreen,
                            checkedTrackColor = NeonGreen.copy(alpha = 0.4f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = CardBorder
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Parameters config panel button
                    IconButton(onClick = { showParamsPanel = !showParamsPanel }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Parameters Control",
                            tint = if (showParamsPanel) CyberBlue else TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBg)
                .verticalScroll(scrollState)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Animated Configuration Parameters slider controls
            AnimatedVisibility(
                visible = showParamsPanel,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBg)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "MATH PARAMETER CONTROLS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberBlue,
                        letterSpacing = 1.sp
                    )
                    Divider(color = CardBorder)

                    // Path loss exponent slider (n)
                    val n by viewModel.pathLossExponent.collectAsState()
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Path Loss Exponent (n)", fontSize = 12.sp, color = TextPrimary)
                            Text(text = String.format("%.2f", n), fontSize = 12.sp, color = NeonGreen, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = n,
                            onValueChange = { viewModel.pathLossExponent.value = it },
                            valueRange = 1.5f..4.5f,
                            colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen)
                        )
                    }

                    // EMA Parameter controls
                    if (selectedFilter == FilterType.EMA) {
                        val alpha by viewModel.emaAlpha.collectAsState()
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "EMA Smoothing factor (Alpha)", fontSize = 12.sp, color = TextPrimary)
                                Text(text = String.format("%.2f", alpha), fontSize = 12.sp, color = CyberBlue, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = alpha,
                                onValueChange = { viewModel.emaAlpha.value = it },
                                valueRange = 0.05f..0.95f,
                                colors = SliderDefaults.colors(thumbColor = CyberBlue, activeTrackColor = CyberBlue)
                            )
                        }
                    }

                    // Kalman Parameter controls
                    if (selectedFilter == FilterType.KALMAN) {
                        val q by viewModel.kalmanQ.collectAsState()
                        val r by viewModel.kalmanR.collectAsState()
                        
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Kalman Process Noise (Q)", fontSize = 12.sp, color = TextPrimary)
                                Text(text = String.format("%.4f", q), fontSize = 12.sp, color = CyberBlue, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = q,
                                onValueChange = { viewModel.kalmanQ.value = it },
                                valueRange = 0.001f..0.2f,
                                colors = SliderDefaults.colors(thumbColor = CyberBlue, activeTrackColor = CyberBlue)
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Kalman Measurement Noise (R)", fontSize = 12.sp, color = TextPrimary)
                                Text(text = String.format("%.2f", r), fontSize = 12.sp, color = CyberBlue, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = r,
                                onValueChange = { viewModel.kalmanR.value = it },
                                valueRange = 0.5f..15.0f,
                                colors = SliderDefaults.colors(thumbColor = CyberBlue, activeTrackColor = CyberBlue)
                            )
                        }
                    }
                }
            }

            // Top Telemetry Header Panel: Simulator Mode, Filter Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Simulation toggle card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "SIGNAL MODE", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text(text = if (isSimulationMode) "SIMULATOR" else "HARDWARE SCAN", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = isSimulationMode,
                            onCheckedChange = { viewModel.setSimulationMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberBlue,
                                checkedTrackColor = CyberBlue.copy(alpha = 0.4f)
                            )
                        )
                    }
                }

                // Global Filter Selector card
                var dropdownExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dropdownExpanded = true }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "ACTIVE ALGORITHM", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text(text = selectedFilter.name, fontSize = 13.sp, color = CyberBlue, fontWeight = FontWeight.Bold)
                            }
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Select Filter",
                                tint = CyberBlue
                            )
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.background(CardBg)
                        ) {
                            FilterType.values().forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(text = filter.name, color = TextPrimary) },
                                    onClick = {
                                        viewModel.setFilterType(filter)
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ----------------- PREMIUM DYNAMIC ACTIVE BEACON PANEL (ORION AESTHETICS) -----------------
            val strongestBeacon by viewModel.strongestBeacon.collectAsState()
            val activeBeaconCount by viewModel.activeBeaconCount.collectAsState()
            val rssiThreshold by viewModel.rssiThreshold.collectAsState()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardBg)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Row: Beacon Icon, Current Location Label, Count
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info, // Location / Info icon
                                contentDescription = null,
                                tint = CyberBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CURRENT LOCATION (POSISI SAAT INI)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "$activeBeaconCount active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeBeaconCount > 0) NeonGreen else AlertRed
                        )
                    }

                    Divider(color = CardBorder)

                    // Beacon Name
                    Text(
                        text = strongestBeacon?.name ?: "Searching for beacons...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (strongestBeacon != null) TextPrimary else TextSecondary
                    )

                    // Signal Strength Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Signal strength (Kekuatan Sinyal)",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = strongestBeacon?.let { "${it.rssi} dBm" } ?: "-- dBm",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (strongestBeacon != null) CyberBlue else TextSecondary
                        )
                    }

                    // Progress Bar / Slider (0 to 100 for -100dBm to -30dBm)
                    val progressRatio = strongestBeacon?.let {
                        val rssiVal = it.rssi.coerceIn(-100, -30)
                        (rssiVal + 100).toFloat() / 70.0f
                    } ?: 0.0f

                    LinearProgressIndicator(
                        progress = progressRatio,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (progressRatio > 0.6f) NeonGreen else CyberBlue,
                        trackColor = CardBorder
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = CardBorder)

                    // RSSI Signal Detection Threshold Slider Element (Dynamic limit)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Signal Detection Threshold (Min. RSSI)",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "${rssiThreshold.toInt()} dBm",
                                fontSize = 12.sp,
                                color = NeonGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = rssiThreshold,
                            onValueChange = { viewModel.rssiThreshold.value = it },
                            valueRange = -100.0f..-40.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyberBlue,
                                activeTrackColor = CyberBlue,
                                inactiveTrackColor = CardBorder
                            )
                        )
                        Text(
                            text = "Slide to filter weak/distant signals (prevents fictional reflections).",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Radar visualization space (Large 2D plot of real-time coordinate matching)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                RadarPositionView(
                    beacons = viewModel.beaconConfigs,
                    estimatedDistances = estimatedDistanceMap,
                    estimatedPosition = estimatedPosition
                )
            }

            // SYSTEM-WIDE TELEMETRY TERMINAL FEED
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "SYSTEM-WIDE TELEMETRY LOGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LogTerminal(
                    logs = systemLogs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )
            }

            // INDIVIDUAL BEACON LOGGERS & METRICS
            viewModel.beaconConfigs.forEach { beacon ->
                val rawRssi = rawRssiMap[beacon.macAddress] ?: 0
                val filteredRssi = filteredRssiMap[beacon.macAddress] ?: 0.0f
                val distance = estimatedDistanceMap[beacon.macAddress] ?: 0.0f
                val isSeen = activeBeaconsMap[beacon.macAddress] ?: false
                
                val rawHistory = viewModel.getRawHistory(beacon.macAddress)
                val filteredHistory = viewModel.getFilteredHistory(beacon.macAddress)
                val logs by viewModel.getLogs(beacon.macAddress).collectAsState(initial = emptyList())

                BeaconCard(
                    config = beacon,
                    rawRssi = rawRssi,
                    filteredRssi = filteredRssi,
                    estimatedDistance = distance,
                    isSeen = isSeen,
                    rawHistory = rawHistory,
                    filteredHistory = filteredHistory,
                    logs = logs,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
