package com.orion.beaconscope.domain.filter

/**
 * Interface representing a mathematical smoothing filter for BLE RSSI signals.
 */
interface SignalFilter {
    /**
     * Resets the filter's state.
     */
    fun reset()

    /**
     * Feeds a new raw RSSI measurement and returns the filtered state estimation.
     */
    fun filter(measurement: Float): Float
}
