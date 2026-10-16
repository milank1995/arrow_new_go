package com.arrowpuzzle.game.model

/**
 * 8-directional compass enum with normalized vector offsets and rotation angles.
 */
enum class Direction(val dx: Float, val dy: Float, val angleDegrees: Float) {
    RIGHT(1f, 0f, 0f),
    DOWN_RIGHT(0.7071f, 0.7071f, 45f),
    DOWN(0f, 1f, 90f),
    DOWN_LEFT(-0.7071f, 0.7071f, 135f),
    LEFT(-1f, 0f, 180f),
    UP_LEFT(-0.7071f, -0.7071f, 225f),
    UP(0f, -1f, 270f),
    UP_RIGHT(0.7071f, -0.7071f, 315f);

    val opposite: Direction
        get() = when (this) {
            RIGHT -> LEFT
            DOWN_RIGHT -> UP_LEFT
            DOWN -> UP
            DOWN_LEFT -> UP_RIGHT
            LEFT -> RIGHT
            UP_LEFT -> DOWN_RIGHT
            UP -> DOWN
            UP_RIGHT -> DOWN_LEFT
        }

    companion object {
        fun fromAngle(angle: Float): Direction {
            val normalized = ((angle % 360f) + 360f) % 360f
            return values().minByOrNull {
                val diff = Math.abs(it.angleDegrees - normalized)
                Math.min(diff, 360f - diff)
            } ?: RIGHT
        }
    }
}
