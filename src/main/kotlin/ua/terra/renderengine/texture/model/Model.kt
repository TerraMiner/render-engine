package ua.terra.renderengine.texture.model

/**
 * Represents texture coordinates for rendering.
 * @property minU Minimum U coordinate (0.0 to 1.0)
 * @property minV Minimum V coordinate (0.0 to 1.0)
 * @property maxU Maximum U coordinate (0.0 to 1.0)
 * @property maxV Maximum V coordinate (0.0 to 1.0)
 */
data class Model(val minU: Float, val minV: Float, val maxU: Float, val maxV: Float) {
    companion object {
        val DEFAULT = Model(0f, 0f, 1f, 1f)
    }
}