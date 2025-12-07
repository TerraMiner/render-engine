package ua.terra.renderengine.camera

/**
 * Camera view bounds for restricting camera movement within a specified area.
 * @property minX Minimum X boundary
 * @property minY Minimum Y boundary
 * @property maxX Maximum X boundary
 * @property maxY Maximum Y boundary
 */
data class CameraBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
)

