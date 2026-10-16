package com.arrowpuzzle.game.model

/**
 * Data structure defining a stage's dimensions, arrows, and difficulty parameters.
 */
data class LevelData(
    val levelNumber: Int,
    val gridWidth: Int,
    val gridHeight: Int,
    val arrows: List<ArrowModel>,
    val movesAllowed: Int = arrows.size + 8,
    val isHardStage: Boolean = (levelNumber % 10 == 0)
)
