package ua.terra.renderengine

/**
 * Defines the type of rendering operation to be performed.
 * Each type is associated with a unique float value used in shaders.
 */
enum class RenderType(val value: Float) {
    /** Texture rendering */
    TEXTURE(0f),
    /** Filled rectangle rendering */
    FILL_RECT(1f),
    /** Hollow (outline) rectangle rendering */
    HOLL_RECT(2f),
    /** Line rendering */
    LINE(3f),
    /** Text rendering */
    TEXT(4f);
}