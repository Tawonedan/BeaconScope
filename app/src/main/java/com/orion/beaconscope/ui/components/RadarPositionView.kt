package com.orion.beaconscope.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orion.beaconscope.data.model.BeaconConfig
import com.orion.beaconscope.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * A beautiful, custom Canvas-based 2D Positioning Radar View.
 * Renders static beacons, dynamic mathematical distance intersection circles,
 * and the user's computed X/Y least-squares coordinates.
 */
@Composable
fun RadarPositionView(
    beacons: List<BeaconConfig>,
    estimatedDistances: Map<String, Float>,
    estimatedPosition: Pair<Float, Float>?, // computed (x, y)
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = TextSecondary, fontSize = 10.sp)
    val textStyleBold = TextStyle(color = TextPrimary, fontSize = 11.sp)

    // Dynamic radar sweeps & user coordinate pulse animations
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val radarSweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(TerminalBg)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
    ) {
        val width = size.width
        val height = size.height
        val padding = 60f

        // Room dimensions mapping parameters (simulate a 8m x 8m grid area)
        val roomWidthMeters = 8.0f
        val roomHeightMeters = 8.0f
        val gridScaleX = (width - 2 * padding) / roomWidthMeters
        val gridScaleY = (height - 2 * padding) / roomHeightMeters

        // Function to map real X/Y meters to Canvas Offset coordinates
        fun getCanvasCoordinates(x: Float, y: Float): Offset {
            val cx = padding + (x * gridScaleX)
            // Flip Y axis so bottom-left is (0,0) and top-left is (0, max_y)
            val cy = height - padding - (y * gridScaleY)
            return Offset(cx, cy)
        }

        // Draw radial concentric radar distance rings (2m, 4m, 6m, 8m)
        val centerPoint = getCanvasCoordinates(4.0f, 4.0f)
        for (radiusMeters in listOf(2.0f, 4.0f, 6.0f, 8.0f)) {
            val pixelRadius = radiusMeters * ((gridScaleX + gridScaleY) / 2.0f)
            drawCircle(
                color = GridColor,
                radius = pixelRadius,
                center = centerPoint,
                style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
            )
        }

        // Draw Sweep Radar Line (Green telemetry scan indicator)
        val rad = Math.toRadians(radarSweepAngle.toDouble())
        val maxScale = 8.0f * ((gridScaleX + gridScaleY) / 2.0f)
        val sweepEnd = Offset(
            x = centerPoint.x + (maxScale * cos(rad)).toFloat(),
            y = centerPoint.y + (maxScale * sin(rad)).toFloat()
        )
        drawLine(
            color = NeonGreen.copy(alpha = 0.15f),
            start = centerPoint,
            end = sweepEnd,
            strokeWidth = 3f
        )

        // Draw Grid Telemetry Coordinates Axes
        drawLine(color = CardBorder, start = Offset(padding, 0f), end = Offset(padding, height), strokeWidth = 2f)
        drawLine(color = CardBorder, start = Offset(0f, height - padding), end = Offset(width, height - padding), strokeWidth = 2f)

        // Axis scale numbers in meters
        for (i in 0..8 step 2) {
            val labelOffset = getCanvasCoordinates(i.toFloat(), 0f)
            drawText(
                textMeasurer = textMeasurer,
                text = "${i}m",
                topLeft = Offset(labelOffset.x - 10f, height - padding + 5f),
                style = textStyle
            )

            val labelOffsetY = getCanvasCoordinates(0f, i.toFloat())
            drawText(
                textMeasurer = textMeasurer,
                text = "${i}m",
                topLeft = Offset(padding - 35f, labelOffsetY.y - 8f),
                style = textStyle
            )
        }

        // 1. Draw dynamic distance circles around static beacons (Visualizing equations intersection)
        beacons.forEach { beacon ->
            val coords = getCanvasCoordinates(beacon.x, beacon.y)
            val distance = estimatedDistances[beacon.macAddress] ?: 0f

            if (distance > 0f) {
                // Convert real physical distance to pixels
                val pixelRadius = distance * ((gridScaleX + gridScaleY) / 2.0f)
                
                // Draw thin colored ring around beacon
                drawCircle(
                    color = CyberBlue.copy(alpha = 0.2f),
                    radius = pixelRadius,
                    center = coords,
                    style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f))
                )
            }
        }

        // 2. Draw Beacons as target nodes
        beacons.forEachIndexed { index, beacon ->
            val coords = getCanvasCoordinates(beacon.x, beacon.y)
            
            // Beacon Outer Ring
            drawCircle(
                color = CardBorder,
                radius = 16f,
                center = coords
            )
            // Beacon Inner Dot
            drawCircle(
                color = if (estimatedDistances.containsKey(beacon.macAddress)) NeonGreen else AlertRed,
                radius = 8f,
                center = coords
            )

            // Text Label (Beacon Name)
            drawText(
                textMeasurer = textMeasurer,
                text = "${beacon.name} (${beacon.x}m, ${beacon.y}m)",
                topLeft = Offset(coords.x + 22f, coords.y - 12f),
                style = textStyleBold
            )
        }

        // 3. Draw Estimated User Location
        if (estimatedPosition != null) {
            val userCanvasCoords = getCanvasCoordinates(estimatedPosition.first, estimatedPosition.second)

            // Dynamic pulsating glow ring
            drawCircle(
                color = CyberBlue.copy(alpha = 0.35f),
                radius = pulseScale * 2f,
                center = userCanvasCoords
            )

            // Estimated User Node
            drawCircle(
                color = CyberBlue,
                radius = 12f,
                center = userCanvasCoords
            )

            drawCircle(
                color = Color.White,
                radius = 4f,
                center = userCanvasCoords
            )

            // Estimated coordinate label text
            val roundedX = String.format("%.2f", estimatedPosition.first)
            val roundedY = String.format("%.2f", estimatedPosition.second)
            drawText(
                textMeasurer = textMeasurer,
                text = "USER: ($roundedX, $roundedY)",
                topLeft = Offset(userCanvasCoords.x - 50f, userCanvasCoords.y - 45f),
                style = TextStyle(color = CyberBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
        } else {
            // Draw calibrating / searching warning text
            drawText(
                textMeasurer = textMeasurer,
                text = "AWAITING POSITION TELEMETRY...",
                topLeft = Offset(centerPoint.x - 90f, height - padding - 40f),
                style = TextStyle(color = TextSecondary, fontSize = 10.sp, letterSpacing = 2.sp)
            )
        }
    }
}
