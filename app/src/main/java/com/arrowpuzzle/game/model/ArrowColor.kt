package com.arrowpuzzle.game.model

import android.graphics.Color

/**
 * Visual color themes for puzzle arrows.
 */
enum class ArrowColor(val primaryHex: Int, val gradientHex: Int, val borderHex: Int) {
    CYAN(
        Color.parseColor("#06B6D4"),
        Color.parseColor("#0891B2"),
        Color.parseColor("#E0F2FE")
    ),
    AMBER(
        Color.parseColor("#F59E0B"),
        Color.parseColor("#D97706"),
        Color.parseColor("#FEF3C7")
    ),
    PURPLE(
        Color.parseColor("#8B5CF6"),
        Color.parseColor("#7C3AED"),
        Color.parseColor("#EDE9FE")
    ),
    EMERALD(
        Color.parseColor("#10B981"),
        Color.parseColor("#059669"),
        Color.parseColor("#D1FAE5")
    ),
    CORAL(
        Color.parseColor("#EF4444"),
        Color.parseColor("#DC2626"),
        Color.parseColor("#FEE2E2")
    ),
    INDIGO(
        Color.parseColor("#6366F1"),
        Color.parseColor("#4F46E5"),
        Color.parseColor("#E0E7FF")
    );

    companion object {
        fun forIndex(index: Int): ArrowColor {
            val list = values()
            return list[index % list.size]
        }
    }
}
