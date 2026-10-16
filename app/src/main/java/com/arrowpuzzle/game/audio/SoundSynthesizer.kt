package com.arrowpuzzle.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedural low-latency audio synthesizer using native Android AudioTrack.
 *
 * Generates dynamic, crystal-clear 44.1kHz sound effects without requiring
 * external sound assets or bulky third-party audio libraries.
 */
object SoundSynthesizer {

    private const val SAMPLE_RATE = 44100
    private val scope = CoroutineScope(Dispatchers.Default)
    var isSoundEnabled: Boolean = true

    /**
     * Launch whoosh: Rapid frequency sweep from 450Hz to 900Hz with smooth decay.
     */
    fun playLaunchWhoosh() {
        if (!isSoundEnabled) return
        scope.launch {
            val durationMs = 150
            val numSamples = (SAMPLE_RATE * (durationMs / 1000f)).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / numSamples
                val freq = 450.0 + (900.0 - 450.0) * progress
                val envelope = (1.0 - progress) * (1.0 - progress)
                val sample = (sin(2.0 * PI * freq * t) * envelope * Short.MAX_VALUE * 0.7).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        }
    }

    /**
     * Blocked collision: Low 120Hz resonant impact thud.
     */
    fun playBlockedThud() {
        if (!isSoundEnabled) return
        scope.launch {
            val durationMs = 130
            val numSamples = (SAMPLE_RATE * (durationMs / 1000f)).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / numSamples
                val freq = 120.0 * (1.0 - progress * 0.3)
                val envelope = exp(-progress * 8.0)
                val sample = (sin(2.0 * PI * freq * t) * envelope * Short.MAX_VALUE * 0.85).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        }
    }

    /**
     * Combo chords: Harmonized pentatonic scale tones based on combo streak.
     */
    fun playCombo(comboIndex: Int) {
        if (!isSoundEnabled) return
        scope.launch {
            // Pentatonic scale: C5, D5, E5, G5, A5, C6, E6
            val frequencies = doubleArrayOf(523.25, 587.33, 659.25, 783.99, 880.00, 1046.50, 1318.51)
            val baseFreq = frequencies[comboIndex.coerceIn(0, frequencies.size - 1)]
            val durationMs = 220
            val numSamples = (SAMPLE_RATE * (durationMs / 1000f)).toInt()
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / numSamples
                val envelope = exp(-progress * 5.0)
                // Fundamental + harmonic overtone
                val sample = ((sin(2.0 * PI * baseFreq * t) * 0.7 +
                        sin(2.0 * PI * (baseFreq * 2.0) * t) * 0.3) *
                        envelope * Short.MAX_VALUE * 0.75).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        }
    }

    /**
     * Level victory: Celebratory ascending tri-chord arpeggio.
     */
    fun playVictory() {
        if (!isSoundEnabled) return
        scope.launch {
            val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
            val noteDurationMs = 120
            val totalDurationMs = notes.size * noteDurationMs + 200
            val numSamples = (SAMPLE_RATE * (totalDurationMs / 1000f)).toInt()
            val buffer = ShortArray(numSamples)

            for (n in notes.indices) {
                val freq = notes[n]
                val startSample = (n * noteDurationMs * SAMPLE_RATE / 1000)
                val noteSamples = (noteDurationMs * 2.5 * SAMPLE_RATE / 1000).toInt()

                for (i in 0 until noteSamples) {
                    val idx = startSample + i
                    if (idx >= numSamples) break
                    val t = i.toDouble() / SAMPLE_RATE
                    val progress = i.toDouble() / noteSamples
                    val envelope = exp(-progress * 4.0)
                    val s = (sin(2.0 * PI * freq * t) * envelope * Short.MAX_VALUE * 0.4).toInt()
                    buffer[idx] = (buffer[idx] + s).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            playPcm(buffer)
        }
    }

    /**
     * UI Click: Subtle 25ms tactile tick.
     */
    fun playClick() {
        if (!isSoundEnabled) return
        scope.launch {
            val durationMs = 25
            val numSamples = (SAMPLE_RATE * (durationMs / 1000f)).toInt()
            val buffer = ShortArray(numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val progress = i.toDouble() / numSamples
                val envelope = 1.0 - progress
                val sample = (sin(2.0 * PI * 1200.0 * t) * envelope * Short.MAX_VALUE * 0.4).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playPcm(buffer)
        }
    }

    private fun playPcm(pcmData: ShortArray) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcmData.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(pcmData, 0, pcmData.size)
            audioTrack.play()
            // Release after playback finishes
            scope.launch {
                val delayMs = (pcmData.size * 1000L / SAMPLE_RATE) + 100
                kotlinx.coroutines.delay(delayMs)
                audioTrack.release()
            }
        } catch (_: Exception) {
        }
    }
}
