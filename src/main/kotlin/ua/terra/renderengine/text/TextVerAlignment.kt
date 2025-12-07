package ua.terra.renderengine.text

/**
 * Vertical alignment for text rendering.
 * @property offset The alignment offset factor
 */
enum class TextVerAlignment(val offset: Float) {
    /** Align text upward */
    UP(-1f),
    /** Align text to the bottom */
    BOTTOM(1f)
}