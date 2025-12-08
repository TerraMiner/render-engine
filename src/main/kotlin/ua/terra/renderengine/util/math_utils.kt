package ua.terra.renderengine.util

import kotlin.math.PI

/**
 * Linear interpolation between two values.
 * @param start Starting value
 * @param end Ending value
 * @param alpha Interpolation factor from 0 to 1
 * @return Interpolated value
 */
fun lerp(start: Float, end: Float, alpha: Float): Float {
    return start + (end - start) * alpha
}

/**
 * Angle interpolation with correct rotation (shortest path).
 * @param start Starting angle in radians
 * @param end Ending angle in radians
 * @param alpha Interpolation factor from 0 to 1
 * @return Interpolated angle
 */
fun lerpAngle(start: Float, end: Float, alpha: Float): Float {
    var diff = end - start
    while (diff > PI) diff -= (PI * 2).toFloat()
    while (diff < -PI) diff += (PI * 2).toFloat()
    return start + diff * alpha
}