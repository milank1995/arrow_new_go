package com.arrowpuzzle.game

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arrowpuzzle.game.audio.SoundSynthesizer
import com.arrowpuzzle.game.databinding.ActivityMainBinding
import com.arrowpuzzle.game.engine.LevelManager
import com.arrowpuzzle.game.haptics.HapticController
import com.arrowpuzzle.game.model.ArrowModel
import com.arrowpuzzle.game.model.LevelData
import com.arrowpuzzle.game.storage.GamePreferences
import com.arrowpuzzle.game.ui.ArrowBoardView
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity(), ArrowBoardView.GameListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: GamePreferences
    private lateinit var haptics: HapticController

    private var currentLevelData: LevelData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = GamePreferences(this)
        haptics = HapticController(this)

        // Initialize audio and haptic flags from saved preferences
        SoundSynthesizer.isSoundEnabled = prefs.soundEnabled
        haptics.isHapticEnabled = prefs.hapticEnabled

        binding.arrowBoardView.listener = this
        binding.arrowBoardView.hapticController = haptics

        setupTopBar()
        setupBoosters()
        loadLevel(prefs.currentLevel)
    }

    private fun setupTopBar() {
        updateHud()

        binding.btnSettings.setOnClickListener {
            SoundSynthesizer.playClick()
            haptics.vibrateTap()
            showSettingsDialog()
        }

        binding.btnLives.setOnClickListener {
            Toast.makeText(this, "Lives regenerate automatically every 15 min", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBoosters() {
        binding.btnHint.setOnClickListener {
            if (binding.arrowBoardView.triggerHint()) {
                Toast.makeText(this, "Clearable arrow highlighted!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAuto.setOnClickListener {
            binding.arrowBoardView.triggerAutoClear(3)
        }

        binding.btnBomb.setOnClickListener {
            SoundSynthesizer.playClick()
            haptics.vibrateTap()
            Toast.makeText(this, "Tap a blocked arrow to clear obstruction", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadLevel(levelNum: Int) {
        val level = LevelManager.getLevel(levelNum)
        currentLevelData = level
        binding.tvLevelTitle.text = getString(R.string.level_label, levelNum)
        binding.tvRemainingCount.text = getString(R.string.arrows_left, level.arrows.size)
        binding.arrowBoardView.loadLevel(level)
        updateHud()
    }

    private fun updateHud() {
        binding.tvLives.text = "${prefs.lives}/${GamePreferences.MAX_LIVES}"
        binding.tvIqScore.text = "IQ: ${prefs.iqScore}"
    }

    override fun onArrowCleared(clearedArrow: ArrowModel, remainingCount: Int, comboStreak: Int) {
        binding.tvRemainingCount.text = getString(R.string.arrows_left, remainingCount)
        if (comboStreak > 1) {
            prefs.iqScore += 1
            binding.tvIqScore.text = "IQ: ${prefs.iqScore}"
        }
    }

    override fun onArrowBlocked(blockedArrow: ArrowModel) {
        // Decrement life on collision error
        if (prefs.lives > 0) {
            prefs.lives -= 1
            updateHud()
        }
        if (prefs.lives <= 0) {
            showGameOverDialog()
        }
    }

    override fun onLevelCompleted() {
        val iqBonus = 20
        prefs.iqScore += iqBonus
        prefs.coins += 25
        prefs.currentLevel += 1
        updateHud()

        showVictoryDialog(iqBonus)
    }

    private fun showVictoryDialog(iqGained: Int) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_victory)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val tvIq = dialog.findViewById<TextView>(R.id.tvIqGained)
        val btnNext = dialog.findViewById<Button>(R.id.btnNextLevel)

        tvIq.text = "+$iqGained IQ"
        btnNext.setOnClickListener {
            SoundSynthesizer.playClick()
            haptics.vibrateTap()
            dialog.dismiss()
            loadLevel(prefs.currentLevel)
        }

        dialog.show()
    }

    private fun showGameOverDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_game_over)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnTryAgain = dialog.findViewById<Button>(R.id.btnTryAgain)
        btnTryAgain.setOnClickListener {
            SoundSynthesizer.playClick()
            haptics.vibrateTap()
            prefs.lives = GamePreferences.MAX_LIVES
            updateHud()
            dialog.dismiss()
            currentLevelData?.let { binding.arrowBoardView.loadLevel(it) }
        }

        dialog.show()
    }

    private fun showSettingsDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_settings)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val switchSound = dialog.findViewById<MaterialSwitch>(R.id.switchSound)
        val switchHaptic = dialog.findViewById<MaterialSwitch>(R.id.switchHaptic)
        val btnReset = dialog.findViewById<Button>(R.id.btnReset)
        val btnClose = dialog.findViewById<Button>(R.id.btnClose)

        switchSound.isChecked = prefs.soundEnabled
        switchHaptic.isChecked = prefs.hapticEnabled

        switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.soundEnabled = isChecked
            SoundSynthesizer.isSoundEnabled = isChecked
        }

        switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            prefs.hapticEnabled = isChecked
            haptics.isHapticEnabled = isChecked
        }

        btnReset.setOnClickListener {
            prefs.resetProgress()
            updateHud()
            dialog.dismiss()
            loadLevel(1)
            Toast.makeText(this, "Progress reset to Level 1", Toast.LENGTH_SHORT).show()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
