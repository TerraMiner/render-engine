package ua.terra.renderengine.render

import kotlin.math.PI

/**
 * Object state data for interpolation (position, scale, rotation).
 * Used for smooth rendering between physics ticks.
 * @property x X coordinate
 * @property y Y coordinate
 * @property rotation Rotation in radians
 * @property scaleX Horizontal scale
 * @property scaleY Vertical scale
 */
data class RenderState(
    var x: Float = 0f,
    var y: Float = 0f,
    var rotation: Float = 0f,
    var scaleX: Float = 1f,
    var scaleY: Float = 1f
) {
    /**
     * Copies all values from another RenderState.
     * @param other Source state to copy from
     */
    fun copyFrom(other: RenderState) {
        this.x = other.x
        this.y = other.y
        this.rotation = other.rotation
        this.scaleX = other.scaleX
        this.scaleY = other.scaleY
    }

    companion object {
        /**
         * Interpolates between two states.
         * @param previous Previous state
         * @param current Current state
         * @param alpha Interpolation factor from 0 to 1 (partial tick)
         * @return Interpolated state
         */
        fun interpolate(previous: RenderState, current: RenderState, alpha: Float): RenderState {
            return RenderState(
                x = lerp(previous.x, current.x, alpha),
                y = lerp(previous.y, current.y, alpha),
                rotation = lerpAngle(previous.rotation, current.rotation, alpha),
                scaleX = lerp(previous.scaleX, current.scaleX, alpha),
                scaleY = lerp(previous.scaleY, current.scaleY, alpha)
            )
        }

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
    }
}

