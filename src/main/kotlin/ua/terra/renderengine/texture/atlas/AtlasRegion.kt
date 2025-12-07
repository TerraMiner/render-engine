package ua.terra.renderengine.texture.atlas

/**
 * Represents a rectangular region within a texture atlas with both UV and pixel coordinates.
 * @property minU Minimum U coordinate (0.0 to 1.0)
 * @property minV Minimum V coordinate (0.0 to 1.0)
 * @property maxU Maximum U coordinate (0.0 to 1.0)
 * @property maxV Maximum V coordinate (0.0 to 1.0)
 * @property pixelX X-coordinate in pixels
 * @property pixelY Y-coordinate in pixels
 * @property pixelWidth Width in pixels
 * @property pixelHeight Height in pixels
 */
data class AtlasRegion(
    val minU: Float,
    val minV: Float,
    val maxU: Float,
    val maxV: Float,
    val pixelX: Int,
    val pixelY: Int,
    val pixelWidth: Int,
    val pixelHeight: Int
)