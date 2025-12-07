package ua.terra.renderengine.text

/**
 * Horizontal position of text relative to its anchor point.
 * @property offset The position offset factor
 */
enum class TextHorPosition(val offset: Float) {
    /** Position text to the left of the anchor */
    LEFT(1f),
    /** Center text on the anchor */
    CENTER(0.5f),
    /** Position text to the right of the anchor */
    RIGHT(0f)
}