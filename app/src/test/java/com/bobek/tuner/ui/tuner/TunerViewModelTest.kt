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

package com.bobek.tuner.ui.tuner

import com.bobek.tuner.domain.DetectedNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TunerViewModelTest {

    @Test
    fun initialStateIsIdle() {
        val viewModel = TunerViewModel()
        assertEquals(TunerState.Idle, viewModel.getTunerStateFlow().value)
    }

    @Test
    fun stopListeningWithoutStartingDoesNotThrowAndStaysIdle() {
        val viewModel = TunerViewModel()
        viewModel.stopListening()
        assertEquals(TunerState.Idle, viewModel.getTunerStateFlow().value)
    }
}

class FrequencySmoothingTest {

    @Test
    fun nullFrequencyReturnsNull() {
        val smoothing = FrequencySmoothing(alpha = 0.5f)
        assertNull(smoothing.process(null))
    }

    @Test
    fun firstFrequencyIsReturnedUnsmoothed() {
        val smoothing = FrequencySmoothing(alpha = 0.5f)
        assertEquals(100f, smoothing.process(100f))
    }

    @Test
    fun subsequentFrequencyIsExponentiallySmoothed() {
        val smoothing = FrequencySmoothing(alpha = 0.5f)
        smoothing.process(100f)
        assertEquals(150f, smoothing.process(200f))
        assertEquals(175f, smoothing.process(200f))
    }

    @Test
    fun nullFrequencyResetsSmoothing() {
        val smoothing = FrequencySmoothing(alpha = 0.5f)
        smoothing.process(100f)
        smoothing.process(200f)
        assertNull(smoothing.process(null))
        assertEquals(300f, smoothing.process(300f))
    }
}

class NoteConfirmationGateTest {

    private val noteA = DetectedNote(name = "A", octave = 4, frequency = 440f, cents = 0)
    private val noteAUpdated = DetectedNote(name = "A", octave = 4, frequency = 442f, cents = 8)
    private val noteB = DetectedNote(name = "B", octave = 4, frequency = 493.88f, cents = 0)
    private val noteC = DetectedNote(name = "C", octave = 5, frequency = 523.25f, cents = 0)

    @Test
    fun nullNoteReturnsNull() {
        val gate = NoteConfirmationGate(confirmationFrames = 3)
        assertNull(gate.process(null))
    }

    @Test
    fun firstNoteIsConfirmedImmediately() {
        val gate = NoteConfirmationGate(confirmationFrames = 3)
        assertEquals(noteA, gate.process(noteA))
    }

    @Test
    fun sameNoteUpdatesConfirmedNoteImmediately() {
        val gate = NoteConfirmationGate(confirmationFrames = 3)
        gate.process(noteA)
        assertEquals(noteAUpdated, gate.process(noteAUpdated))
    }

    @Test
    fun differentNoteIsNotConfirmedBeforeReachingConfirmationFrames() {
        val gate = NoteConfirmationGate(confirmationFrames = 3)
        gate.process(noteA)

        assertEquals(noteA, gate.process(noteB))
        assertEquals(noteA, gate.process(noteB))
    }

    @Test
    fun differentNoteIsConfirmedAfterReachingConfirmationFrames() {
        val gate = NoteConfirmationGate(confirmationFrames = 3)
        gate.process(noteA)

        gate.process(noteB)
        gate.process(noteB)
        assertEquals(noteB, gate.process(noteB))
    }

    @Test
    fun flickeringCandidateNoteDoesNotDestabilizeConfirmedNote() {
        val gate = NoteConfirmationGate(confirmationFrames = 3)
        gate.process(noteA)

        gate.process(noteB)
        assertEquals(noteA, gate.process(noteC))
        assertEquals(noteA, gate.process(noteB))
    }

    @Test
    fun nullNoteResetsCandidateAndConfirmedNote() {
        val gate = NoteConfirmationGate(confirmationFrames = 3)
        gate.process(noteA)
        gate.process(noteB)

        assertNull(gate.process(null))
        assertEquals(noteB, gate.process(noteB))
    }
}
