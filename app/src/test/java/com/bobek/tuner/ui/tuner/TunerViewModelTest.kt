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

import com.bobek.tuner.data.AppNightMode
import com.bobek.tuner.domain.DEFAULT_REFERENCE_PITCH
import com.bobek.tuner.domain.DetectedNote
import com.bobek.tuner.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val DEBOUNCE = 1.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class TunerViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = UnconfinedTestDispatcher(testScheduler)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        settingsRepository: SettingsRepository = FakeSettingsRepository()
    ): TunerViewModel = TunerViewModel(settingsRepository)

    @Test
    fun initialStateIsIdle() {
        val viewModel = createViewModel()
        assertEquals(TunerState.Idle, viewModel.getTunerStateFlow().value)
    }

    @Test
    fun stopListeningWithoutStartingDoesNotThrowAndStaysIdle() {
        val viewModel = createViewModel()
        viewModel.stopListening()
        assertEquals(TunerState.Idle, viewModel.getTunerStateFlow().value)
    }

    @Test
    fun initialReferencePitchLoadsFromSettings() = runTest(testDispatcher) {
        val settings = FakeSettingsRepository(referencePitch = 442)
        val viewModel = createViewModel(settings)
        assertEquals(442, viewModel.getReferencePitchFlow().value)
    }

    @Test
    fun setReferencePitchUpdatesFlow() {
        val viewModel = createViewModel()
        viewModel.setReferencePitch(442)
        assertEquals(442, viewModel.getReferencePitchFlow().value)
    }

    @Test
    fun referencePitchPersistedToSettingsAfterDebounce() = runTest(testDispatcher) {
        val settings = FakeSettingsRepository()
        val viewModel = createViewModel(settings)

        viewModel.setReferencePitch(442)
        advanceTimeBy(DEBOUNCE + 1.milliseconds)

        assertEquals(442, settings.writtenReferencePitch)
    }

    @Test
    fun initialReferencePitchNotPersistedToSettings() = runTest(testDispatcher) {
        val settings = FakeSettingsRepository()
        createViewModel(settings)

        advanceTimeBy(DEBOUNCE + 1.milliseconds)

        assertFalse(settings.referencePitchWritten)
    }

    @Test
    fun initialSolfegeNotationLoadsFromSettings() = runTest(testDispatcher) {
        val settings = FakeSettingsRepository(solfegeNotation = true)
        val viewModel = createViewModel(settings)
        assertTrue(viewModel.getSolfegeNotationFlow().value)
    }

    @Test
    fun setSolfegeNotationUpdatesFlow() {
        val viewModel = createViewModel()
        viewModel.setSolfegeNotation(true)
        assertTrue(viewModel.getSolfegeNotationFlow().value)
    }

    @Test
    fun solfegeNotationPersistedToSettingsAfterDebounce() = runTest(testDispatcher) {
        val settings = FakeSettingsRepository()
        val viewModel = createViewModel(settings)

        viewModel.setSolfegeNotation(true)
        advanceTimeBy(DEBOUNCE + 1.milliseconds)

        assertEquals(true, settings.writtenSolfegeNotation)
    }

    @Test
    fun initialSolfegeNotationNotPersistedToSettings() = runTest(testDispatcher) {
        val settings = FakeSettingsRepository()
        createViewModel(settings)

        advanceTimeBy(DEBOUNCE + 1.milliseconds)

        assertFalse(settings.solfegeNotationWritten)
    }
}

private class FakeSettingsRepository(
    referencePitch: Int = DEFAULT_REFERENCE_PITCH,
    solfegeNotation: Boolean = false
) : SettingsRepository {

    private val referencePitchFlow = MutableStateFlow(referencePitch)
    private val solfegeNotationFlow = MutableStateFlow(solfegeNotation)

    var referencePitchWritten = false
        private set
    var writtenReferencePitch: Int? = null
        private set
    var solfegeNotationWritten = false
        private set
    var writtenSolfegeNotation: Boolean? = null
        private set

    override fun getNightMode() = MutableStateFlow(AppNightMode.FOLLOW_SYSTEM)
    override suspend fun setNightMode(nightMode: AppNightMode) = Unit

    override fun getReferencePitch(): Flow<Int> = referencePitchFlow
    override suspend fun setReferencePitch(referencePitch: Int) {
        referencePitchWritten = true
        writtenReferencePitch = referencePitch
    }

    override fun getSolfegeNotation(): Flow<Boolean> = solfegeNotationFlow
    override suspend fun setSolfegeNotation(solfegeNotation: Boolean) {
        solfegeNotationWritten = true
        writtenSolfegeNotation = solfegeNotation
    }
}

class OnsetDetectorTest {

    private fun silence(size: Int = 8) = FloatArray(size)
    private fun tone(amplitude: Float, size: Int = 8) = FloatArray(size) { amplitude }

    @Test
    fun silenceIsNotAnOnset() {
        val detector = OnsetDetector(energyRatio = 2.5f, minRms = 0.01f)
        assertFalse(detector.isOnset(silence()))
    }

    @Test
    fun suddenLoudSignalAfterSilenceIsAnOnset() {
        val detector = OnsetDetector(energyRatio = 2.5f, minRms = 0.01f)
        detector.isOnset(silence())
        assertTrue(detector.isOnset(tone(amplitude = 0.5f)))
    }

    @Test
    fun sustainedSignalIsNotAnOnsetOnFollowingFrames() {
        val detector = OnsetDetector(energyRatio = 2.5f, minRms = 0.01f)
        detector.isOnset(tone(amplitude = 0.5f))
        assertFalse(detector.isOnset(tone(amplitude = 0.5f)))
    }

    @Test
    fun decayingSignalIsNotAnOnset() {
        val detector = OnsetDetector(energyRatio = 2.5f, minRms = 0.01f)
        detector.isOnset(tone(amplitude = 0.5f))
        assertFalse(detector.isOnset(tone(amplitude = 0.3f)))
    }

    @Test
    fun quietSignalBelowMinRmsIsNotAnOnset() {
        val detector = OnsetDetector(energyRatio = 2.5f, minRms = 0.01f)
        detector.isOnset(silence())
        assertFalse(detector.isOnset(tone(amplitude = 0.005f)))
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
    fun firstNoteRequiresConfirmationFramesBeforeBeingConfirmed() {
        val gate = NoteConfirmationGate(confirmationFrames = 3)

        assertNull(gate.process(noteA))
        assertNull(gate.process(noteA))
        assertEquals(noteA, gate.process(noteA))
    }

    @Test
    fun sameNoteUpdatesConfirmedNoteImmediately() {
        val gate = NoteConfirmationGate(confirmationFrames = 3)
        gate.confirm(noteA)

        assertEquals(noteAUpdated, gate.process(noteAUpdated))
    }

    @Test
    fun differentNoteIsNotConfirmedBeforeReachingConfirmationFrames() {
        val gate = NoteConfirmationGate(confirmationFrames = 3)
        gate.confirm(noteA)

        assertEquals(noteA, gate.process(noteB))
        assertEquals(noteA, gate.process(noteB))
    }

    @Test
    fun differentNoteIsConfirmedAfterReachingConfirmationFrames() {
        val gate = NoteConfirmationGate(confirmationFrames = 3)
        gate.confirm(noteA)

        gate.process(noteB)
        gate.process(noteB)
        assertEquals(noteB, gate.process(noteB))
    }

    @Test
    fun flickeringCandidateNoteDoesNotDestabilizeConfirmedNote() {
        val gate = NoteConfirmationGate(confirmationFrames = 3)
        gate.confirm(noteA)

        gate.process(noteB)
        assertEquals(noteA, gate.process(noteC))
        assertEquals(noteA, gate.process(noteB))
    }

    @Test
    fun nullNoteResetsCandidateAndConfirmedNote() {
        val gate = NoteConfirmationGate(confirmationFrames = 3)
        gate.confirm(noteA)
        gate.process(noteB)

        assertNull(gate.process(null))
        assertEquals(noteB, gate.confirm(noteB))
    }

    private fun NoteConfirmationGate.confirm(note: DetectedNote): DetectedNote? {
        repeat(2) { process(note) }
        return process(note)
    }
}
