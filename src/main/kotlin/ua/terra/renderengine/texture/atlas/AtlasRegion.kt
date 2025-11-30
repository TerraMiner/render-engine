package ua.terra.renderengine.texture.atlas

data class AtlasRegion(
    val uvX: Float,
    val uvY: Float,
    val uvMX: Float,
    val uvMY: Float,
    val pixelX: Int,
    val pixelY: Int,
    val pixelWidth: Int,
    val pixelHeight: Int
)