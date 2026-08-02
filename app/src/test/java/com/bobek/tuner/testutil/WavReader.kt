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

package com.bobek.tuner.testutil

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decoded PCM audio, normalized to -1.0..1.0 and downmixed to mono. Not a data class:
 * FloatArray equality is reference-based, which would make generated equals()/hashCode() misleading.
 */
class WavAudio(val sampleRate: Int, val samples: FloatArray)

/** Minimal RIFF/WAVE parser: 16-bit int or 32-bit float PCM, mono or multichannel downmixed to mono. */
object WavReader {

    fun read(bytes: ByteArray): WavAudio {
        require(bytes.size >= 44) { "File too small to be a valid WAV file" }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        require(readChunkId(buffer) == "RIFF") { "Not a RIFF file" }
        buffer.int // overall chunk size, unused
        require(readChunkId(buffer) == "WAVE") { "Not a WAVE file" }

        var audioFormat = 0
        var channels = 0
        var sampleRate = 0
        var bitsPerSample = 0
        var dataBytes: ByteArray? = null

        while (buffer.remaining() >= 8) {
            val chunkId = readChunkId(buffer)
            val chunkSize = buffer.int
            when (chunkId) {
                "fmt " -> {
                    val chunkStart = buffer.position()
                    audioFormat = buffer.short.toInt() and 0xFFFF
                    channels = buffer.short.toInt() and 0xFFFF
                    sampleRate = buffer.int
                    buffer.int // byte rate, unused
                    buffer.short // block align, unused
                    bitsPerSample = buffer.short.toInt() and 0xFFFF
                    buffer.position(chunkStart + chunkSize + (chunkSize % 2))
                }

                "data" -> {
                    dataBytes = ByteArray(chunkSize)
                    buffer.get(dataBytes)
                    if (chunkSize % 2 == 1 && buffer.hasRemaining()) buffer.get() // pad byte
                }

                else -> buffer.position((buffer.position() + chunkSize + (chunkSize % 2)).coerceAtMost(buffer.limit()))
            }
        }

        val data = requireNotNull(dataBytes) { "WAV file has no data chunk" }
        val samples = decodeSamples(data, audioFormat, bitsPerSample, channels)
        return WavAudio(sampleRate, samples)
    }

    private fun readChunkId(buffer: ByteBuffer): String {
        val idBytes = ByteArray(4)
        buffer.get(idBytes)
        return String(idBytes, Charsets.US_ASCII)
    }

    private fun decodeSamples(data: ByteArray, audioFormat: Int, bitsPerSample: Int, channels: Int): FloatArray {
        require(channels > 0) { "WAV file declares zero channels" }
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val bytesPerSample = bitsPerSample / 8
        val frameCount = data.size / (bytesPerSample * channels)
        return FloatArray(frameCount) {
            var sum = 0f
            repeat(channels) { sum += readSample(buffer, audioFormat, bitsPerSample) }
            sum / channels
        }
    }

    private fun readSample(buffer: ByteBuffer, audioFormat: Int, bitsPerSample: Int): Float = when (audioFormat) {
        3 if bitsPerSample == 32 -> buffer.float
        1 if bitsPerSample == 16 -> buffer.short / 32768f
        1 if bitsPerSample == 8 -> ((buffer.get().toInt() and 0xFF) - 128) / 128f
        else -> error("Unsupported WAV format: audioFormat=$audioFormat bitsPerSample=$bitsPerSample")
    }
}
