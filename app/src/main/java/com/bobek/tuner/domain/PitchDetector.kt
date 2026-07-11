/*
 * This file is part of Tuner.
 * Copyright (C) 2026 Philipp Bobek <philipp.bobek@mailbox.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tuner is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.bobek.tuner.domain

// YIN pitch detection algorithm (de Cheveigné & Kawahara, 2002).
object PitchDetector {

    private const val YIN_THRESHOLD = 0.15f
    private const val MIN_FREQUENCY = 30f
    private const val MAX_FREQUENCY = 2000f

    /** Returns the detected fundamental frequency in Hz, or null if no pitch was found. */
    fun detect(buffer: FloatArray, sampleRate: Int): Float? {
        val halfBufferSize = buffer.size / 2
        val maxLag = minOf(halfBufferSize, (sampleRate / MIN_FREQUENCY).toInt())
        val minLag = maxOf(2, (sampleRate / MAX_FREQUENCY).toInt())

        if (minLag >= maxLag) return null

        val normalizedDifference = computeNormalizedDifference(buffer, halfBufferSize, maxLag)
        val lag = findLagBelowThreshold(normalizedDifference, minLag, maxLag) ?: return null
        val refinedLag = refineWithParabolicInterpolation(normalizedDifference, lag, maxLag)

        return sampleRate / refinedLag
    }

    // Steps 1 & 2: difference function + cumulative mean normalization
    private fun computeNormalizedDifference(buffer: FloatArray, halfBufferSize: Int, maxLag: Int): FloatArray {
        val normalizedDifference = FloatArray(maxLag)
        normalizedDifference[0] = 1f

        var cumulativeSum = 0f
        for (lag in 1 until maxLag) {
            var squaredDifferenceSum = 0f
            for (index in 0 until halfBufferSize) {
                val sampleDifference = buffer[index] - buffer[index + lag]
                squaredDifferenceSum += sampleDifference * sampleDifference
            }
            cumulativeSum += squaredDifferenceSum
            normalizedDifference[lag] = if (cumulativeSum == 0f) 1f else squaredDifferenceSum * lag / cumulativeSum
        }

        return normalizedDifference
    }

    // Step 3: find the first lag below threshold that is a local minimum
    private fun findLagBelowThreshold(normalizedDifference: FloatArray, minLag: Int, maxLag: Int): Int? {
        var lag = minLag
        while (lag < maxLag - 1) {
            if (normalizedDifference[lag] < YIN_THRESHOLD) {
                while (lag + 1 < maxLag && normalizedDifference[lag + 1] < normalizedDifference[lag]) lag++
                return lag
            }
            lag++
        }
        return null
    }

    // Step 4: parabolic interpolation to refine the period estimate
    private fun refineWithParabolicInterpolation(normalizedDifference: FloatArray, lag: Int, maxLag: Int): Float {
        if (lag !in 1 until maxLag - 1) return lag.toFloat()

        val previousDiff = normalizedDifference[lag - 1]
        val currentDiff = normalizedDifference[lag]
        val nextDiff = normalizedDifference[lag + 1]
        val denominator = 2f * currentDiff - nextDiff - previousDiff

        return lag + if (denominator != 0f) (nextDiff - previousDiff) / (2f * denominator) else 0f
    }
}
