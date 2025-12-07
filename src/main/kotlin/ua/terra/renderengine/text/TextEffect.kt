package ua.terra.renderengine.text

/**
 * Visual effects that can be applied to text rendering.
 */
enum class TextEffect(val value: Float) {
    /** Default rendering without special effects */
    DEFAULT(0f),
    /** Rainbow color effect */
    RAINBOW(1f),
    /** Chroma effect */
    CHROMA(2f)
}