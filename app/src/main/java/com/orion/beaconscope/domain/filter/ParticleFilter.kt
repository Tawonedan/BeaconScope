package com.orion.beaconscope.domain.filter

import kotlin.math.exp
import kotlin.random.Random

/**
 * 1-Dimensional Particle Filter (Sequential Monte Carlo) for experimental
 * state estimation under non-Gaussian or heavy multipath signal conditions.
 */
class ParticleFilter(
    val numParticles: Int = 100,
    var processNoise: Float = 0.5f,
    var measurementNoise: Float = 4.0f
) : SignalFilter {

    private val random = java.util.Random()
    private var initialized = false
    private val particles = FloatArray(numParticles)
    private val weights = FloatArray(numParticles)

    override fun reset() {
        initialized = false
    }

    override fun filter(measurement: Float): Float {
        if (!initialized) {
            // Initialize particles randomly around the first reading
            for (i in 0 until numParticles) {
                particles[i] = measurement + (random.nextGaussian().toFloat() * measurementNoise)
                weights[i] = 1.0f / numParticles
            }
            initialized = true
            return measurement
        }

        // 1. Prediction: Add process noise to all particles
        for (i in 0 until numParticles) {
            particles[i] += random.nextGaussian().toFloat() * processNoise
        }

        // 2. Weighting: Calculate weight based on Gaussian probability density function
        var sumWeights = 0.0f
        for (i in 0 until numParticles) {
            val diff = particles[i] - measurement
            // Gaussian likelihood weight: w = exp(-diff^2 / (2 * noise^2))
            val w = exp(-(diff * diff) / (2.0f * measurementNoise * measurementNoise))
            weights[i] = w
            sumWeights += w
        }

        // Normalize weights, with fallback to uniform if sum is zero
        if (sumWeights > 1e-6f) {
            for (i in 0 until numParticles) {
                weights[i] /= sumWeights
            }
        } else {
            for (i in 0 until numParticles) {
                weights[i] = 1.0f / numParticles
            }
        }

        // 3. Resampling (Systematic Roulette Wheel Resampling)
        val cumulativeWeights = FloatArray(numParticles)
        cumulativeWeights[0] = weights[0]
        for (i in 1 until numParticles) {
            cumulativeWeights[i] = cumulativeWeights[i - 1] + weights[i]
        }

        val resampledParticles = FloatArray(numParticles)
        val step = 1.0f / numParticles
        val start = random.nextFloat() * step
        var c = cumulativeWeights[0]
        var idx = 0

        for (i in 0 until numParticles) {
            val u = start + i * step
            while (u > c && idx < numParticles - 1) {
                idx++
                c = cumulativeWeights[idx]
            }
            resampledParticles[i] = particles[idx]
        }

        // Copy back resampled particles
        System.arraycopy(resampledParticles, 0, particles, 0, numParticles)

        // 4. State Estimation: Return average of particle cloud
        var estimatedState = 0.0f
        for (i in 0 until numParticles) {
            estimatedState += particles[i]
        }
        return estimatedState / numParticles
    }
}
