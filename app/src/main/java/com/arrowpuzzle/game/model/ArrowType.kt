package com.arrowpuzzle.game.model

/**
 * Functional mechanics classification for arrow elements.
 */
enum class ArrowType {
    NORMAL,        // Standard single-direction launch
    DUAL_HEAD,     // Bi-directional arrow requiring clearance on both sides
    OBSTACLE,      // Non-interactive blocker or locked barrier
    BOMB           // Clearable bomb that destroys adjacent obstacles
}
