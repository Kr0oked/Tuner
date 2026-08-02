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
import com.bobek.tuner.domain.PitchDetector
import com.bobek.tuner.testutil.WavAudio
import com.bobek.tuner.testutil.WavReader
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

// Thresholds calibrated with headroom above the observed worst case across all fixtures (5 cents / 1.84 stddev).
private const val MAX_SETTLED_DEVIATION_CENTS = 8
private const val MAX_SETTLED_JITTER_STD_DEV = 3.0

/**
 * Fixture cents in filenames are the actually-measured tuner offset, not a nominal
 * target - a reference tuner can't be dialed exactly.
 */
class GuitarSampleReplayTest {

    @Test
    fun replayRealGuitarRecordingsThroughPitchPipeline() {
        val fixtures = fixtureFiles()
        if (fixtures.isEmpty()) {
            println("No guitar sample fixtures found under guitar_samples/ - skipping. See guitar_samples/README.md.")
            return
        }

        val failures = mutableListOf<String>()
        for (file in fixtures) {
            val target = parseTarget(file)
            val audio = WavReader.read(file.readBytes())
            val frameStates = replay(audio)

            val confirmedCentsInTargetRegion = frameStates
                .filter { it != null && it.name == target.noteName && it.octave == target.octave }
                .map { it!!.cents }

            if (confirmedCentsInTargetRegion.isEmpty()) {
                failures += "${file.name}: never confirmed target note ${target.noteName}${target.octave}"
                continue
            }

            // Skip the first half: the post-pluck pitch-glide is a real acoustic effect, not jitter to fix.
            val settled = confirmedCentsInTargetRegion.drop(confirmedCentsInTargetRegion.size / 2)
            val maxDeviation = settled.maxOf { abs(it - target.cents) }
            val jitter = standardDeviation(settled)

            if (maxDeviation > MAX_SETTLED_DEVIATION_CENTS) {
                failures += "${file.name}: settled cents deviate up to $maxDeviation from target " +
                        "${target.cents} (limit $MAX_SETTLED_DEVIATION_CENTS), settled=$settled"
            }
            if (jitter > MAX_SETTLED_JITTER_STD_DEV) {
                failures += "${file.name}: settled jitter stddev $jitter exceeds limit " +
                        "$MAX_SETTLED_JITTER_STD_DEV, settled=$settled"
            }
        }

        if (failures.isNotEmpty()) {
            fail("Guitar sample replay found issues:\n" + failures.joinToString("\n"))
        }
    }

    /** Mirrors TunerViewModel.recordSession()'s per-frame pipeline and constants exactly. */
    private fun replay(audio: WavAudio): List<DetectedNote?> {
        require(audio.sampleRate == SAMPLE_RATE) {
            "Fixture sample rate ${audio.sampleRate} does not match expected $SAMPLE_RATE"
        }

        val onsetDetector = OnsetDetector(ONSET_ENERGY_RATIO, ONSET_MIN_RMS)
        val smoothing = FrequencySmoothing(FREQUENCY_SMOOTHING_ALPHA)
        val gate = NoteConfirmationGate(NOTE_CONFIRMATION_FRAMES)

        val frameStates = mutableListOf<DetectedNote?>()
        var lastState: DetectedNote? = null
        var offset = 0
        while (offset + ANALYSIS_SIZE <= audio.samples.size) {
            val frame = audio.samples.copyOfRange(offset, offset + ANALYSIS_SIZE)
            offset += ANALYSIS_SIZE
            // Onset frames are skipped entirely (state stays unchanged), matching recordSession().
            if (!onsetDetector.isOnset(frame)) {
                val frequency = PitchDetector.detect(frame, SAMPLE_RATE)
                val note = smoothing.process(frequency)?.let { DetectedNote.fromFrequency(it) }
                lastState = gate.process(note)
            }
            frameStates += lastState
        }
        return frameStates
    }

    private fun standardDeviation(values: List<Int>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
    }

    private data class FixtureTarget(val noteName: String, val octave: Int, val cents: Int)

    private fun fixtureFiles(): List<File> {
        val directoryUrl = javaClass.classLoader?.getResource("guitar_samples") ?: return emptyList()
        val directory = File(directoryUrl.toURI())
        return directory.listFiles { file -> file.extension.equals("wav", ignoreCase = true) }
            ?.sortedBy { it.name }
            ?: emptyList()
    }

    private fun parseTarget(file: File): FixtureTarget {
        val match = requireNotNull(FILENAME_PATTERN.matchEntire(file.name)) {
            "Fixture filename doesn't match <note><octave>_<signed-cents>cents.wav convention: ${file.name}"
        }
        val (name, octave, cents) = match.destructured
        return FixtureTarget(name.uppercase(), octave.toInt(), cents.toInt())
    }

    private companion object {
        val FILENAME_PATTERN = Regex("""^([A-G]#?)(\d+)_([+-]?\d+)cents\.wav$""", RegexOption.IGNORE_CASE)
    }
}
