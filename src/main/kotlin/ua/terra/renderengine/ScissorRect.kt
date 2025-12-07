package ua.terra.renderengine

/**
 * Represents a rectangular scissor region for clipping rendering.
 * @property x X coordinate of the scissor rectangle
 * @property y Y coordinate of the scissor rectangle
 * @property width Width of the scissor rectangle
 * @property height Height of the scissor rectangle
 */
data class ScissorRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

