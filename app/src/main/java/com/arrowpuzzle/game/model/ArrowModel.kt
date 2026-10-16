package com.arrowpuzzle.game.model

import android.graphics.PointF
import android.graphics.RectF

/**
 * Core model representing a single interactive arrow piece on the puzzle board.
 */
data class ArrowModel(
    val id: String,
    var gridX: Float,
    var gridY: Float,
    val length: Float = 1.0f,
    val direction: Direction,
    val color: ArrowColor = ArrowColor.CYAN,
    val type: ArrowType = ArrowType.NORMAL,
    var isLaunched: Boolean = false,
    var isRemoved: Boolean = false,
    var isWiggling: Boolean = false,
    var launchProgress: Float = 0f,
    var wiggleProgress: Float = 0f,
    var wiggleOffsetX: Float = 0f,
    var wiggleOffsetY: Float = 0f,
    var isHighlighted: Boolean = false
) {
    /**
     * Head (front tip) coordinate of the arrow.
     */
    fun getHeadPoint(cellSize: Float, originX: Float, originY: Float): PointF {
        val halfLen = (length * cellSize) / 2f
        val cx = originX + gridX * cellSize + cellSize / 2f
        val cy = originY + gridY * cellSize + cellSize / 2f
        return PointF(
            cx + direction.dx * halfLen,
            cy + direction.dy * halfLen
        )
    }

    /**
     * Tail (rear base) coordinate of the arrow.
     */
    fun getTailPoint(cellSize: Float, originX: Float, originY: Float): PointF {
        val halfLen = (length * cellSize) / 2f
        val cx = originX + gridX * cellSize + cellSize / 2f
        val cy = originY + gridY * cellSize + cellSize / 2f
        return PointF(
            cx - direction.dx * halfLen,
            cy - direction.dy * halfLen
        )
    }

    /**
     * Bounding box in grid coordinates (for touch and collision checks).
     */
    fun getGridBounds(): RectF {
        val halfLen = length / 2f
        val hx = gridX + direction.dx * halfLen
        val hy = gridY + direction.dy * halfLen
        val tx = gridX - direction.dx * halfLen
        val ty = gridY - direction.dy * halfLen
        val minX = Math.min(hx, tx) - 0.35f
        val maxX = Math.max(hx, tx) + 0.35f
        val minY = Math.min(hy, ty) - 0.35f
        val maxY = Math.max(hy, ty) + 0.35f
        return RectF(minX, minY, maxX, maxY)
    }
}
