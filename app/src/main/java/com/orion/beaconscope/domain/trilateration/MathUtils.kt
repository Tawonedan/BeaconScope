package com.orion.beaconscope.domain.trilateration

import kotlin.math.pow

/**
 * Pure mathematical helper functions for path-loss and matrix calculations.
 */
object MathUtils {

    /**
     * Estimates physical distance in meters using the log-distance path loss model.
     */
    fun calculateDistance(rssi: Float, txPower: Int, n: Float): Float {
        if (rssi >= 0f) return 0.1f // Impossible positive signal
        return 10.0f.pow((txPower - rssi) / (10.0f * n))
    }

    /**
     * Solves the linear system A * x = b using standard Least-Squares pseudo-inverse.
     * Computes: x = (A^T * A)^(-1) * A^T * b
     * Specially optimized for 2x2 systems (2 equations, 2 unknowns: X and Y).
     *
     * A is a 2x2 matrix:
     * [ A00  A01 ]
     * [ A10  A11 ]
     *
     * b is a 2x1 vector: [ b0, b1 ]
     *
     * Returns a Pair(X, Y) representing the coordinates, or null if matrix is singular (collinear beacons).
     */
    fun solveLeastSquares2D(
        a00: Float, a01: Float,
        a10: Float, a11: Float,
        b0: Float, b1: Float
    ): Pair<Float, Float>? {
        // Since we are solving A * x = b, and A is already 2x2, we can invert A directly if it's invertible!
        // Direct matrix inversion is much faster and identical to least squares when equations are exact.
        // Let's compute determinant: det = a00 * a11 - a01 * a10
        val det = a00 * a11 - a01 * a10
        if (kotlin.math.abs(det) < 1e-6f) {
            // Beacons are collinear, cannot determine unique 2D position!
            return null
        }

        // Inverted A matrix elements:
        // [  a11/det  -a01/det ]
        // [ -a10/det   a00/det ]
        val inv00 = a11 / det
        val inv01 = -a01 / det
        val inv10 = -a10 / det
        val inv11 = a00 / det

        // Multiply A^-1 * b
        val x = inv00 * b0 + inv01 * b1
        val y = inv10 * b0 + inv11 * b1

        return Pair(x, y)
    }
}
