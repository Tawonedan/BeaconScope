package com.orion.beaconscope.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.beaconscope.data.model.LogEntry
import com.orion.beaconscope.data.model.LogType
import com.orion.beaconscope.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A beautiful, technical dark-themed monospaced scrolling log terminal.
 * Used for live diagnostic analysis of signal and distance estimations.
 */
@Composable
fun LogTerminal(
    logs: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    // Auto-scroll logic to snap to the latest telemetry entries
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TerminalBg)
            .padding(8.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(logs) { log ->
                val timeString = timeFormat.format(Date(log.timestamp))
                
                val logColor = when (log.type) {
                    LogType.INFO -> CyberBlue
                    LogType.RSSI -> RawSignalColor
                    LogType.FILTER -> NeonGreen
                    LogType.TRILATERATION -> YellowOrange
                    LogType.ERROR -> AlertRed
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                ) {
                    Text(
                        text = "[$timeString] ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = log.message,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = logColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
