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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectedNoteTest {

    @Test
    fun fromFrequencyRecognizesConcertPitchA4() {
        val note = DetectedNote.fromFrequency(440f)
        assertEquals("A", note.name)
        assertEquals(4, note.octave)
        assertEquals(0, note.cents)
        assertEquals(440f, note.frequency)
    }

    @Test
    fun fromFrequencyRecognizesOctaveBelowAndAbove() {
        assertEquals(DetectedNote("A", 3, 220f, 0), DetectedNote.fromFrequency(220f))
        assertEquals(DetectedNote("A", 5, 880f, 0), DetectedNote.fromFrequency(880f))
    }

    @Test
    fun fromFrequencyRecognizesNoteAtOctaveBoundary() {
        val belowC = DetectedNote.fromFrequency(246.941651f)
        assertEquals("B", belowC.name)
        assertEquals(3, belowC.octave)

        val atC = DetectedNote.fromFrequency(261.625565f)
        assertEquals("C", atC.name)
        assertEquals(4, atC.octave)
    }

    @Test
    fun fromFrequencyRecognizesLowestNote() {
        val note = DetectedNote.fromFrequency(16.351597f)
        assertEquals("C", note.name)
        assertEquals(0, note.octave)
        assertEquals(0, note.cents)
    }

    @Test
    fun fromFrequencySharpOfNoteYieldsPositiveCents() {
        val note = DetectedNote.fromFrequency(445f)
        assertEquals("A", note.name)
        assertEquals(4, note.octave)
        assertEquals(20, note.cents)
    }

    @Test
    fun fromFrequencyFlatOfNoteYieldsNegativeCents() {
        val note = DetectedNote.fromFrequency(435f)
        assertEquals("A", note.name)
        assertEquals(4, note.octave)
        assertEquals(-20, note.cents)
    }

    @Test
    fun isSameNoteIsTrueForSameNameAndOctaveRegardlessOfFrequencyAndCents() {
        val a = DetectedNote(name = "A", octave = 4, frequency = 440f, cents = 0)
        val aSlightlySharp = DetectedNote(name = "A", octave = 4, frequency = 442f, cents = 8)
        assertTrue(a.isSameNote(aSlightlySharp))
    }

    @Test
    fun isSameNoteIsFalseForDifferentOctave() {
        val a4 = DetectedNote(name = "A", octave = 4, frequency = 440f, cents = 0)
        val a3 = DetectedNote(name = "A", octave = 3, frequency = 220f, cents = 0)
        assertFalse(a4.isSameNote(a3))
    }

    @Test
    fun isSameNoteIsFalseForDifferentName() {
        val a4 = DetectedNote(name = "A", octave = 4, frequency = 440f, cents = 0)
        val b4 = DetectedNote(name = "B", octave = 4, frequency = 493.88f, cents = 0)
        assertFalse(a4.isSameNote(b4))
    }

    @Test
    fun isSameNoteIsFalseForNull() {
        val a4 = DetectedNote(name = "A", octave = 4, frequency = 440f, cents = 0)
        assertFalse(a4.isSameNote(null))
    }
}
