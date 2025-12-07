package ua.terra.renderengine.text

/**
 * Vertical position of text relative to its anchor point.
 * @property offset The position offset factor
 */
enum class TextVerPosition(val offset: Float) {
    /** Position text above the anchor */
    UP(1f),
    /** Center text vertically on the anchor */
    CENTER(.5f),
    /** Position text below the anchor */
    BOTTOM(0f)
}