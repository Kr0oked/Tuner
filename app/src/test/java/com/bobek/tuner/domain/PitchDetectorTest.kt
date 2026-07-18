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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

private const val SAMPLE_RATE = 44100
private const val BUFFER_SIZE = 4096

class PitchDetectorTest {

    @Test
    fun detectsConcertPitchA4() {
        assertDetectsFrequency(440f)
    }

    @Test
    fun detectsLowGuitarStringE2() {
        assertDetectsFrequency(82.41f)
    }

    @Test
    fun detectsHighNoteNearUpperRangeLimit() {
        assertDetectsFrequency(1567.98f)
    }

    @Test
    fun detectsMidRangeFrequency() {
        assertDetectsFrequency(100f)
    }

    @Test
    fun returnsNullForSilence() {
        val buffer = FloatArray(BUFFER_SIZE)
        assertNull(PitchDetector.detect(buffer, SAMPLE_RATE))
    }

    @Test
    fun returnsNullWhenBufferTooShortForDetectableRange() {
        val buffer = sineWave(440f, size = 2)
        assertNull(PitchDetector.detect(buffer, SAMPLE_RATE))
    }

    private fun assertDetectsFrequency(frequency: Float) {
        val buffer = sineWave(frequency, BUFFER_SIZE)
        val detected = PitchDetector.detect(buffer, SAMPLE_RATE)
        assertEquals(frequency, detected!!, frequency * 0.01f)
    }

    private fun sineWave(frequency: Float, size: Int): FloatArray =
        FloatArray(size) { index -> sin(2.0 * PI * frequency * index / SAMPLE_RATE).toFloat() }
}