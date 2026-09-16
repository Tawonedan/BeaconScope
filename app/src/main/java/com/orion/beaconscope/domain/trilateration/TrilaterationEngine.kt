package com.orion.beaconscope.domain.trilateration

import android.util.Log

/**
 * Trilateration Engine that calculates the estimated 2D position (X, Y)
 * of a mobile device based on distance measurements from three static beacons.
 */
class TrilaterationEngine {

    companion object {
        private const val TAG = "TrilaterationEngine"
    }

    /**
     * Calculates the estimated 2D coordinates (X, Y).
     *
     * @param x1, y1 Coordinates of Beacon 1
     * @param d1     Estimated physical distance to Beacon 1
     * @param x2, y2 Coordinates of Beacon 2
     * @param d2     Estimated physical distance to Beacon 2
     * @param x3, y3 Coordinates of Beacon 3
     * @param d3     Estimated physical distance to Beacon 3
     *
     * @return Pair(X, Y) representing estimated user location, or null if calculation fails.
     */
    fun calculatePosition2D(
        x1: Float, y1: Float, d1: Float,
        x2: Float, y2: Float, d2: Float,
        x3: Float, y3: Float, d3: Float
    ): Pair<Float, Float>? {
        // System of Linear Equations representation: A * x = b
        // Row 0:
        val a00 = 2.0f * (x1 - x3)
        val a01 = 2.0f * (y1 - y3)
        
        // Row 1:
        val a10 = 2.0f * (x2 - x3)
        val a11 = 2.0f * (y2 - y3)

        // Vector values b0 and b1:
        val b0 = (x1 * x1 - x3 * x3) + (y1 * y1 - y3 * y3) + (d3 * d3 - d1 * d1)
        val b1 = (x2 * x2 - x3 * x3) + (y2 * y2 - y3 * y3) + (d3 * d3 - d2 * d2)

        val position = MathUtils.solveLeastSquares2D(
            a00 = a00, a01 = a01,
            a10 = a10, a11 = a11,
            b0 = b0, b1 = b1
        )

        if (position == null) {
            Log.w(TAG, "Collinear beacon coordinates or singular matrix. Position calculation failed.")
            return null
        }

        // Room bounds constraint (roughly 0m to 10m based on indoor standard, but let's allow moderate spillover)
        val boundedX = position.first.coerceIn(-2.0f, 12.0f)
        val boundedY = position.second.coerceIn(-2.0f, 12.0f)

        return Pair(boundedX, boundedY)
    }
}
