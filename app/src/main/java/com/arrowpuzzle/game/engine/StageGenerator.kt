package com.arrowpuzzle.game.engine

import com.arrowpuzzle.game.model.ArrowColor
import com.arrowpuzzle.game.model.ArrowModel
import com.arrowpuzzle.game.model.ArrowType
import com.arrowpuzzle.game.model.Direction
import com.arrowpuzzle.game.model.LevelData
import kotlin.random.Random

/**
 * Procedural reverse-unravelling stage generator.
 *
 * Guarantees that every generated stage is 100% mathematically solvable
 * by building the board in reverse clearance sequence.
 */
object StageGenerator {

    /**
     * Generates a solvable level for the specified [levelNumber].
     */
    fun generate(levelNumber: Int): LevelData {
        val random = Random(levelNumber.toLong() * 31337L)

        // Scale grid and arrow count with level progression
        val gridDim = when {
            levelNumber <= 5 -> 5
            levelNumber <= 15 -> 6
            levelNumber <= 30 -> 7
            levelNumber <= 60 -> 8
            else -> 9
        }

        val targetCount = when {
            levelNumber <= 3 -> 4 + levelNumber * 2   // 6 to 10 arrows
            levelNumber <= 10 -> 10 + (levelNumber - 3) * 2 // 12 to 24 arrows
            levelNumber <= 25 -> 24 + (levelNumber - 10) // 25 to 39 arrows
            else -> Math.min(50, 35 + (levelNumber - 25) / 2)
        }

        val placed = mutableListOf<ArrowModel>()
        val directions = Direction.values()
        val colors = ArrowColor.values()

        val occupiedPositions = mutableSetOf<Pair<Int, Int>>()

        var attempts = 0
        var idCounter = 1

        while (placed.size < targetCount && attempts < 800) {
            attempts++
            val gx = random.nextInt(gridDim - 2) + 1
            val gy = random.nextInt(gridDim - 2) + 1
            val posKey = Pair(gx, gy)

            if (occupiedPositions.contains(posKey)) continue

            // Shuffle directions to test which one has a clear exit ray
            val shuffledDirs = directions.clone()
            shuffledDirs.shuffle(random)

            var validDir: Direction? = null
            for (dir in shuffledDirs) {
                val candidate = ArrowModel(
                    id = "a_${idCounter}",
                    gridX = gx.toFloat(),
                    gridY = gy.toFloat(),
                    direction = dir,
                    color = colors[(placed.size) % colors.size]
                )

                // Verify that candidate has an open path out of the currently placed arrows
                if (CollisionDetector.isPathClear(candidate, placed, gridDim, gridDim)) {
                    validDir = dir
                    break
                }
            }

            if (validDir != null) {
                val arrowType = if (levelNumber > 12 && random.nextFloat() < 0.15f) {
                    ArrowType.DUAL_HEAD
                } else {
                    ArrowType.NORMAL
                }

                val newArrow = ArrowModel(
                    id = "a_${idCounter++}",
                    gridX = gx.toFloat(),
                    gridY = gy.toFloat(),
                    length = 1.0f,
                    direction = validDir,
                    color = colors[(placed.size) % colors.size],
                    type = arrowType
                )

                placed.add(newArrow)
                occupiedPositions.add(posKey)
            }
        }

        // Reversing the list ensures that the player solves the outermost arrows first
        val boardArrows = placed.reversed()
        val moves = boardArrows.size + Math.max(5, boardArrows.size / 4)

        return LevelData(
            levelNumber = levelNumber,
            gridWidth = gridDim,
            gridHeight = gridDim,
            arrows = boardArrows,
            movesAllowed = moves,
            isHardStage = (levelNumber % 10 == 0)
        )
    }
}
