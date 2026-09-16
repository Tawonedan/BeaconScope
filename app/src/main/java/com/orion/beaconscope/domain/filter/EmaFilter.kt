package com.orion.beaconscope.domain.filter

/**
 * Exponential Moving Average (EMA) filter implementation.
 * Extremely lightweight and effective for simple low-pass noise removal.
 */
class EmaFilter(var alpha: Float = 0.2f) : SignalFilter {

    private var initialized = false
    private var lastEstimate = 0.0f

    init {
        alpha = alpha.coerceIn(0.0f, 1.0f)
    }

    override fun reset() {
        initialized = false
        lastEstimate = 0.0f
    }

    override fun filter(measurement: Float): Float {
        if (!initialized) {
            lastEstimate = measurement
            initialized = true
            return measurement
        }
        val estimate = alpha * measurement + (1.0f - alpha) * lastEstimate
        lastEstimate = estimate
        return estimate
    }
}
