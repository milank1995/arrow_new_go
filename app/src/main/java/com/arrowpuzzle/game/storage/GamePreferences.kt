package com.arrowpuzzle.game.storage

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages player progression, currency, settings, and life regeneration.
 */
class GamePreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("arrow_flow_prefs", Context.MODE_PRIVATE)

    companion object {
        const val MAX_LIVES = 5
        const val LIFE_RESTORE_TIME_MS = 15 * 60 * 1000L // 15 minutes
    }

    var currentLevel: Int
        get() = prefs.getInt("current_level", 1)
        set(value) = prefs.edit().putInt("current_level", value).apply()

    var iqScore: Int
        get() = prefs.getInt("iq_score", 100)
        set(value) = prefs.edit().putInt("iq_score", value).apply()

    var coins: Int
        get() = prefs.getInt("coins", 150)
        set(value) = prefs.edit().putInt("coins", value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var hapticEnabled: Boolean
        get() = prefs.getBoolean("haptic_enabled", true)
        set(value) = prefs.edit().putBoolean("haptic_enabled", value).apply()

    var lives: Int
        get() {
            restoreLivesIfNeeded()
            return prefs.getInt("lives", MAX_LIVES)
        }
        set(value) {
            prefs.edit().putInt("lives", value.coerceIn(0, MAX_LIVES)).apply()
            if (value < MAX_LIVES && lastLifeTime == 0L) {
                lastLifeTime = System.currentTimeMillis()
            }
        }

    private var lastLifeTime: Long
        get() = prefs.getLong("last_life_time", 0L)
        set(value) = prefs.edit().putLong("last_life_time", value).apply()

    private fun restoreLivesIfNeeded() {
        val currentLives = prefs.getInt("lives", MAX_LIVES)
        if (currentLives >= MAX_LIVES) return

        val lastTime = lastLifeTime
        if (lastTime == 0L) return

        val now = System.currentTimeMillis()
        val elapsed = now - lastTime
        val restored = (elapsed / LIFE_RESTORE_TIME_MS).toInt()

        if (restored > 0) {
            val newLives = (currentLives + restored).coerceAtMost(MAX_LIVES)
            prefs.edit().putInt("lives", newLives).apply()
            if (newLives >= MAX_LIVES) {
                lastLifeTime = 0L
            } else {
                lastLifeTime = lastTime + (restored * LIFE_RESTORE_TIME_MS)
            }
        }
    }

    fun resetProgress() {
        prefs.edit().clear().apply()
        lives = MAX_LIVES
    }
}
