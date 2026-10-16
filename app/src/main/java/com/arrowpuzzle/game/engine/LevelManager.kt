package com.arrowpuzzle.game.engine

import com.arrowpuzzle.game.model.ArrowColor
import com.arrowpuzzle.game.model.ArrowModel
import com.arrowpuzzle.game.model.ArrowType
import com.arrowpuzzle.game.model.Direction
import com.arrowpuzzle.game.model.LevelData

/**
 * Manages stage catalog, tutorial stages, and progression levels.
 */
object LevelManager {

    /**
     * Loads level data for [levelNumber].
     */
    fun getLevel(levelNumber: Int): LevelData {
        return when (levelNumber) {
            1 -> getTutorialLevel1()
            2 -> getTutorialLevel2()
            3 -> getTutorialLevel3()
            else -> StageGenerator.generate(levelNumber)
        }
    }

    private fun getTutorialLevel1(): LevelData {
        // 4 outwardly pointing arrows (Simple introductory puzzle)
        val arrows = listOf(
            ArrowModel("t1_1", 2f, 1f, 1.0f, Direction.UP, ArrowColor.CYAN),
            ArrowModel("t1_2", 3f, 2f, 1.0f, Direction.RIGHT, ArrowColor.AMBER),
            ArrowModel("t1_3", 2f, 3f, 1.0f, Direction.DOWN, ArrowColor.PURPLE),
            ArrowModel("t1_4", 1f, 2f, 1.0f, Direction.LEFT, ArrowColor.EMERALD)
        )
        return LevelData(1, 5, 5, arrows, movesAllowed = 8, isHardStage = false)
    }

    private fun getTutorialLevel2(): LevelData {
        // Simple blocking demonstration: horizontal arrow must be cleared before vertical
        val arrows = listOf(
            // Top arrow pointing UP is blocked by horizontal arrow
            ArrowModel("t2_1", 2f, 3f, 1.0f, Direction.UP, ArrowColor.CYAN),
            // Horizontal arrow pointing RIGHT is completely clear
            ArrowModel("t2_2", 2f, 1f, 1.0f, Direction.RIGHT, ArrowColor.AMBER),
            // Outer helper arrow
            ArrowModel("t2_3", 4f, 2f, 1.0f, Direction.DOWN, ArrowColor.EMERALD),
            ArrowModel("t2_4", 1f, 4f, 1.0f, Direction.LEFT, ArrowColor.PURPLE),
            ArrowModel("t2_5", 3f, 4f, 1.0f, Direction.RIGHT, ArrowColor.CORAL)
        )
        return LevelData(2, 6, 6, arrows, movesAllowed = 10, isHardStage = false)
    }

    private fun getTutorialLevel3(): LevelData {
        // Chain reaction clearing
        val arrows = listOf(
            ArrowModel("t3_1", 1f, 1f, 1.0f, Direction.RIGHT, ArrowColor.CYAN),
            ArrowModel("t3_2", 3f, 1f, 1.0f, Direction.DOWN, ArrowColor.AMBER),
            ArrowModel("t3_3", 3f, 3f, 1.0f, Direction.LEFT, ArrowColor.EMERALD),
            ArrowModel("t3_4", 1f, 3f, 1.0f, Direction.UP, ArrowColor.PURPLE),
            // Center clearable arrow that breaks the cycle
            ArrowModel("t3_5", 2f, 2f, 1.0f, Direction.DOWN_RIGHT, ArrowColor.INDIGO),
            ArrowModel("t3_6", 4f, 4f, 1.0f, Direction.RIGHT, ArrowColor.CORAL),
            ArrowModel("t3_7", 0f, 2f, 1.0f, Direction.LEFT, ArrowColor.AMBER)
        )
        return LevelData(3, 6, 6, arrows, movesAllowed = 12, isHardStage = false)
    }
}
