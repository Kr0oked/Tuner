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

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
private const val A4_MIDI = 69
private const val A4_FREQ = 440.0

data class DetectedNote(
    val name: String,
    val octave: Int,
    val frequency: Float,
    val cents: Int
) {
    fun isSameNote(other: DetectedNote?): Boolean =
        other != null && name == other.name && octave == other.octave

    companion object {
        fun fromFrequency(frequency: Float): DetectedNote {
            val midiExact = 12.0 * log2(frequency / A4_FREQ) + A4_MIDI
            val midiRounded = midiExact.roundToInt()
            val noteIndex = ((midiRounded % 12) + 12) % 12
            val octave = (midiRounded / 12) - 1
            val noteFreq = A4_FREQ * 2.0.pow((midiRounded - A4_MIDI) / 12.0)
            val cents = (1200.0 * log2(frequency / noteFreq)).roundToInt().coerceIn(-50, 50)
            return DetectedNote(
                name = NOTE_NAMES[noteIndex],
                octave = octave,
                frequency = frequency,
                cents = cents
            )
        }
    }
}
