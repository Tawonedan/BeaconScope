package com.orion.beaconscope.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.beaconscope.data.model.BeaconConfig
import com.orion.beaconscope.data.model.LogEntry
import com.orion.beaconscope.ui.theme.*

/**
 * An advanced technical card containing BLE beacon metrics, Live scrolling
 * signal charts, and a collapsible diagnostic log terminal feed.
 */
@Composable
fun BeaconCard(
    config: BeaconConfig,
    rawRssi: Int,
    filteredRssi: Float,
    estimatedDistance: Float,
    isSeen: Boolean,
    rawHistory: List<Int>,
    filteredHistory: List<Float>,
    logs: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    var expandedLogs by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header Section: Label, MAC, Status Dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = config.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = config.macAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "Major: ${config.major} | Minor: ${config.minor}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberBlue,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Dynamic Status dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (isSeen) NeonGreen else AlertRed)
                    )
                    Text(
                        text = if (isSeen) "ACTIVE" else "OFFLINE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSeen) NeonGreen else AlertRed,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = CardBorder, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(12.dp))

            // Diagnostic stats display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetric(
                    label = "RAW RSSI",
                    value = if (rawRssi == 0) "--" else "${rawRssi} dBm",
                    color = RawSignalColor
                )
                TelemetryMetric(
                    label = "FILTERED RSSI",
                    value = if (filteredRssi == 0.0f) "--" else String.format("%.1f dBm", filteredRssi),
                    color = CyberBlue
                )
                TelemetryMetric(
                    label = "EST. DISTANCE",
                    value = if (estimatedDistance == 0.0f) "--" else String.format("%.2f m", estimatedDistance),
                    color = NeonGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Signal Slider / Progress Indicator
            Column(modifier = Modifier.fillMaxWidth()) {
                val progressRatio = if (filteredRssi == 0.0f) 0.0f else {
                    val rssiVal = filteredRssi.coerceIn(-100.0f, -30.0f)
                    (rssiVal + 100.0f) / 70.0f
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE SIGNAL SLIDER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (filteredRssi == 0.0f) "-- %" else String.format("%.0f%%", progressRatio * 100f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSeen) NeonGreen else TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = progressRatio,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isSeen) NeonGreen else CyberBlue,
                    trackColor = CardBorder
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Miniature Custom Line Graph
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(TerminalBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                if (rawHistory.isNotEmpty()) {
                    LiveRssiGraph(
                        rawRssiHistory = rawHistory,
                        filteredRssiHistory = filteredHistory,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NO TELEMETRY DATA RECEIVED",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expand/Collapse Diagnostic Log trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedLogs = !expandedLogs }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DIAGNOSTIC LOG TERMINAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = 1.sp
                )
                Icon(
                    imageVector = if (expandedLogs) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand Logs",
                    tint = TextPrimary
                )
            }

            // Expanding Monospaced scrolling log terminal
            AnimatedVisibility(
                visible = expandedLogs,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    LogTerminal(
                        logs = logs,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TelemetryMetric(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary,
            letterSpacing = 0.5.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
