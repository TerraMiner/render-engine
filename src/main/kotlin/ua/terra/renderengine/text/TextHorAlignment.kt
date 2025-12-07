package ua.terra.renderengine.text

/**
 * Horizontal alignment for text rendering.
 * @property offset The alignment offset factor (0 = left, 0.5 = center, 1 = right)
 */
enum class TextHorAlignment(val offset: Float) {
    /** Align text to the left */
    LEFT(0f),
    /** Center text horizontally */
    CENTER(0.5f),
    /** Align text to the right */
    RIGHT(1f)
}