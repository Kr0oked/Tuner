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

import android.Manifest.permission.RECORD_AUDIO
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bobek.tuner.domain.DetectedNote
import com.bobek.tuner.domain.PitchDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class TunerState {
    object Idle : TunerState()
    data class Listening(val note: DetectedNote?) : TunerState()
}

interface ITunerViewModel {

    fun getTunerStateFlow(): StateFlow<TunerState>

    @RequiresPermission(RECORD_AUDIO)
    fun startListening()

    fun stopListening()
}

private const val SAMPLE_RATE = 44100
private const val ANALYSIS_SIZE = 4096
private const val FREQUENCY_SMOOTHING_ALPHA = 0.3f
private const val NOTE_CONFIRMATION_FRAMES = 3

@HiltViewModel
class TunerViewModel @Inject constructor() : ViewModel(), ITunerViewModel {

    private val tunerStateFlow = MutableStateFlow<TunerState>(TunerState.Idle)

    private var recordingJob: Job? = null

    override fun getTunerStateFlow() = tunerStateFlow

    @RequiresPermission(RECORD_AUDIO)
    override fun startListening() {
        if (recordingJob?.isActive == true) return
        tunerStateFlow.value = TunerState.Listening(null)
        recordingJob = viewModelScope.launch(Dispatchers.IO) { recordSession() }
    }

    @RequiresPermission(RECORD_AUDIO)
    private suspend fun recordSession() {
        val record = createAudioRecord() ?: run {
            tunerStateFlow.value = TunerState.Idle
            return
        }

        record.startRecording()

        val smoothing = FrequencySmoothing(FREQUENCY_SMOOTHING_ALPHA)
        val gate = NoteConfirmationGate(NOTE_CONFIRMATION_FRAMES)
        val buffer = FloatArray(ANALYSIS_SIZE)

        try {
            while (currentCoroutineContext().isActive) {
                val read = record.read(buffer, 0, ANALYSIS_SIZE, AudioRecord.READ_BLOCKING)
                if (read > 0) {
                    val frequency = PitchDetector.detect(buffer, SAMPLE_RATE)
                    val note = smoothing.process(frequency)?.let { DetectedNote.fromFrequency(it) }
                    tunerStateFlow.value = TunerState.Listening(gate.process(note))
                }
            }
        } finally {
            record.stop()
            record.release()
            tunerStateFlow.value = TunerState.Idle
        }
    }

    override fun stopListening() {
        recordingJob?.cancel()
        recordingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }

    @RequiresPermission(RECORD_AUDIO)
    private fun createAudioRecord(): AudioRecord? {
        val minBufferBytes = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        val bufferBytes = maxOf(minBufferBytes, ANALYSIS_SIZE * Float.SIZE_BYTES * 2)
        val record = AudioRecord(
            AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
            bufferBytes
        )
        return if (record.state == AudioRecord.STATE_INITIALIZED) record else {
            record.release()
            null
        }
    }
}

class ComposeTunerViewModel(
    val tunerState: TunerState = TunerState.Listening(null)
) : ITunerViewModel {
    override fun getTunerStateFlow() = MutableStateFlow(tunerState)
    override fun startListening() = Unit
    override fun stopListening() = Unit
}

internal class FrequencySmoothing(private val alpha: Float) {

    private var smoothed: Float? = null

    fun process(frequency: Float?): Float? {
        smoothed = if (frequency != null) {
            smoothed?.let { alpha * frequency + (1 - alpha) * it } ?: frequency
        } else {
            null
        }
        return smoothed
    }
}

internal class NoteConfirmationGate(private val confirmationFrames: Int) {

    private var confirmedNote: DetectedNote? = null
    private var candidateNote: DetectedNote? = null
    private var candidateFrameCount = 0

    fun process(note: DetectedNote?): DetectedNote? {
        when {
            note == null -> reset()
            confirmedNote == null -> confirmedNote = note
            note.isSameNote(confirmedNote) -> confirmedNote = note
            note.isSameNote(candidateNote) -> {
                candidateFrameCount++
                if (candidateFrameCount >= confirmationFrames) {
                    confirmedNote = note
                    candidateNote = null
                    candidateFrameCount = 0
                }
            }

            else -> {
                candidateNote = note
                candidateFrameCount = 1
            }
        }
        return confirmedNote
    }

    private fun reset() {
        confirmedNote = null
        candidateNote = null
        candidateFrameCount = 0
    }
}
