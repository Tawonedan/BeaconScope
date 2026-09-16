package com.orion.beaconscope.domain.filter

/**
 * 1-Dimensional Kalman Filter for state estimation in noisy BLE environments.
 * Alternately executes the Predict and Update equations.
 */
class KalmanFilter(
    var q: Float = 0.05f, // Process noise covariance
    var r: Float = 3.0f   // Measurement noise covariance
) : SignalFilter {

    private var initialized = false
    private var x = 0.0f  // Estimated value (State)
    private var p = 1.0f  // Estimation error covariance

    override fun reset() {
        initialized = false
        x = 0.0f
        p = 1.0f
    }

    override fun filter(measurement: Float): Float {
        if (!initialized) {
            x = measurement
            p = 1.0f
            initialized = true
            return measurement
        }

        // 1. Predict Step
        // State remains constant for 1D static tracking: x_pred = x
        val pPred = p + q

        // 2. Update Step
        val k = pPred / (pPred + r) // Kalman Gain
        x = x + k * (measurement - x)
        p = (1.0f - k) * pPred

        return x
    }
}
