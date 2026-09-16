package com.orion.beaconscope.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.beaconscope.ui.theme.CyberBlue
import com.orion.beaconscope.ui.theme.GridColor
import com.orion.beaconscope.ui.theme.RawSignalColor
import com.orion.beaconscope.ui.theme.TextSecondary

/**
 * A highly optimized, custom Canvas-based scrolling line graph.
 * Plots Raw vs Filtered RSSI values dynamically over time.
 */
@Composable
fun LiveRssiGraph(
    rawRssiHistory: List<Int>,
    filteredRssiHistory: List<Float>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = TextSecondary, fontSize = 9.sp)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val maxRssi = -40f
        val minRssi = -100f
        val rssiRange = maxRssi - minRssi

        // Function to map RSSI to Y coordinate
        fun getRssiY(rssi: Float): Float {
            val clamped = rssi.coerceIn(minRssi, maxRssi)
            val pct = (clamped - minRssi) / rssiRange
            return height - (pct * height)
        }

        // Draw Telemetry Grid lines (Horizontal lines every 10 dBm)
        val gridStep = 10f
        for (rssi in -100..-40 step gridStep.toInt()) {
            val y = getRssiY(rssi.toFloat())
            drawLine(
                color = GridColor,
                start = Offset(45f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )

            // Draw Y-axis labels
            drawText(
                textMeasurer = textMeasurer,
                text = "${rssi}dBm",
                topLeft = Offset(2f, y - 12f),
                style = textStyle
            )
        }

        // Plot limit parameters
        val historyLimit = 30
        val xStep = (width - 45f) / (historyLimit - 1).toFloat()

        // 1. Draw Raw RSSI history (Subtle transparent pink line)
        if (rawRssiHistory.isNotEmpty()) {
            val rawPath = Path()
            val startIdx = (rawRssiHistory.size - historyLimit).coerceAtLeast(0)
            
            var isFirst = true
            for (i in startIdx until rawRssiHistory.size) {
                val indexInWindow = i - startIdx
                val x = 45f + indexInWindow * xStep
                val y = getRssiY(rawRssiHistory[i].toFloat())

                if (isFirst) {
                    rawPath.moveTo(x, y)
                    isFirst = false
                } else {
                    rawPath.lineTo(x, y)
                }
            }

            drawPath(
                path = rawPath,
                color = RawSignalColor.copy(alpha = 0.45f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // 2. Draw Filtered RSSI history (Bold bright cyber-blue line)
        if (filteredRssiHistory.isNotEmpty()) {
            val filteredPath = Path()
            val startIdx = (filteredRssiHistory.size - historyLimit).coerceAtLeast(0)
            
            var isFirst = true
            for (i in startIdx until filteredRssiHistory.size) {
                val indexInWindow = i - startIdx
                val x = 45f + indexInWindow * xStep
                val y = getRssiY(filteredRssiHistory[i])

                if (isFirst) {
                    filteredPath.moveTo(x, y)
                    isFirst = false
                } else {
                    filteredPath.lineTo(x, y)
                }
            }

            drawPath(
                path = filteredPath,
                color = CyberBlue,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}
