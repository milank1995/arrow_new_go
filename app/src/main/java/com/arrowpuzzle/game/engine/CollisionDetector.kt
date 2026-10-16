package com.arrowpuzzle.game.engine

import android.graphics.PointF
import com.arrowpuzzle.game.model.ArrowModel
import com.arrowpuzzle.game.model.ArrowType
import com.arrowpuzzle.game.model.Direction

/**
 * High-performance geometric clearance and collision validation engine.
 */
object CollisionDetector {

    private const val COLLISION_PADDING = 0.38f

    /**
     * Determines whether [arrow] has an unobstructed exit path off the board.
     *
     * @param arrow The candidate arrow being tapped.
     * @param activeArrows All active, un-cleared arrows currently on the board.
     * @param gridWidth Width of the grid in units.
     * @param gridHeight Height of the grid in units.
     * @return true if the launch path is clear to the board edge; false if blocked.
     */
    fun isPathClear(
        arrow: ArrowModel,
        activeArrows: List<ArrowModel>,
        gridWidth: Int,
        gridHeight: Int
    ): Boolean {
        if (arrow.isRemoved || arrow.isLaunched) return false

        // Check forward path
        val forwardClear = isRayClear(arrow, arrow.direction, activeArrows, gridWidth, gridHeight)
        if (!forwardClear) return false

        // If dual-head, also check reverse path
        if (arrow.type == ArrowType.DUAL_HEAD) {
            val reverseClear = isRayClear(arrow, arrow.direction.opposite, activeArrows, gridWidth, gridHeight)
            if (!reverseClear) return false
        }

        return true
    }

    /**
     * Checks if a directional ray originating from [arrow] exits the board without colliding.
     */
    private fun isRayClear(
        arrow: ArrowModel,
        dir: Direction,
        activeArrows: List<ArrowModel>,
        gridWidth: Int,
        gridHeight: Int
    ): Boolean {
        val startX = arrow.gridX + dir.dx * (arrow.length * 0.5f)
        val startY = arrow.gridY + dir.dy * (arrow.length * 0.5f)

        // Project ray far outside board boundary
        val maxExtent = Math.max(gridWidth, gridHeight) * 2f
        val endX = startX + dir.dx * maxExtent
        val endY = startY + dir.dy * maxExtent

        for (other in activeArrows) {
            if (other.id == arrow.id || other.isRemoved || other.isLaunched) continue

            if (checkRaySegmentIntersection(startX, startY, endX, endY, other)) {
                return false
            }
        }
        return true
    }

    /**
     * Checks if the line segment from (r1x, r1y) to (r2x, r2y) intersects the body of [other].
     */
    private fun checkRaySegmentIntersection(
        r1x: Float, r1y: Float,
        r2x: Float, r2y: Float,
        other: ArrowModel
    ): Boolean {
        val otherHalf = other.length * 0.5f
        val b1x = other.gridX - other.direction.dx * otherHalf
        val b1y = other.gridY - other.direction.dy * otherHalf
        val b2x = other.gridX + other.direction.dx * otherHalf
        val b2y = other.gridY + other.direction.dy * otherHalf

        // Broad-phase bounding box rejection
        val minX = Math.min(b1x, b2x) - COLLISION_PADDING
        val maxX = Math.max(b1x, b2x) + COLLISION_PADDING
        val minY = Math.min(b1y, b2y) - COLLISION_PADDING
        val maxY = Math.max(b1y, b2y) + COLLISION_PADDING

        val rayMinX = Math.min(r1x, r2x)
        val rayMaxX = Math.max(r1x, r2x)
        val rayMinY = Math.min(r1y, r2y)
        val rayMaxY = Math.max(r1y, r2y)

        if (rayMaxX < minX || rayMinX > maxX || rayMaxY < minY || rayMinY > maxY) {
            return false
        }

        // Narrow-phase segment-to-segment intersection
        return lineSegmentsIntersect(r1x, r1y, r2x, r2y, b1x, b1y, b2x, b2y, COLLISION_PADDING)
    }

    private fun lineSegmentsIntersect(
        x1: Float, y1: Float, x2: Float, y2: Float,
        x3: Float, y3: Float, x4: Float, y4: Float,
        padding: Float
    ): Boolean {
        // Quick distance from other body center to ray
        val d1 = orientation(x1, y1, x2, y2, x3, y3)
        val d2 = orientation(x1, y1, x2, y2, x4, y4)
        val d3 = orientation(x3, y3, x4, y4, x1, y1)
        val d4 = orientation(x3, y3, x4, y4, x2, y2)

        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
            ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))
        ) {
            return true
        }

        // Check endpoint proximity within padding
        if (distToSegmentSquared(x3, y3, x1, y1, x2, y2) < padding * padding) return true
        if (distToSegmentSquared(x4, y4, x1, y1, x2, y2) < padding * padding) return true
        return false
    }

    private fun orientation(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float): Float {
        return (y2 - y1) * (x3 - x2) - (x2 - x1) * (y3 - y2)
    }

    private fun distToSegmentSquared(px: Float, py: Float, vx: Float, vy: Float, wx: Float, wy: Float): Float {
        val l2 = (wx - vx) * (wx - vx) + (wy - vy) * (wy - vy)
        if (l2 == 0f) return (px - vx) * (px - vx) + (py - vy) * (py - vy)
        val t = Math.max(0f, Math.min(1f, ((px - vx) * (wx - vx) + (py - vy) * (wy - vy)) / l2))
        val projX = vx + t * (wx - vx)
        val projY = vy + t * (wy - vy)
        return (px - projX) * (px - projX) + (py - projY) * (py - projY)
    }

    /**
     * Returns all currently clearable arrows (used by Hint booster).
     */
    fun findClearableArrows(
        activeArrows: List<ArrowModel>,
        gridWidth: Int,
        gridHeight: Int
    ): List<ArrowModel> {
        return activeArrows.filter { arrow ->
            !arrow.isRemoved && !arrow.isLaunched &&
            isPathClear(arrow, activeArrows, gridWidth, gridHeight)
        }
    }
}
