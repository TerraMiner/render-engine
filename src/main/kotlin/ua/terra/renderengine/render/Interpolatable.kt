package ua.terra.renderengine.render

import ua.terra.renderengine.RenderEngineCore

/**
 * Base class for objects with state interpolation.
 *
 * Used to create smooth animation between physics ticks.
 *
 * Concept:
 * - tick() is called N times per second (typically 60 times)
 * - render() can be called many times between ticks
 * - Interpolation uses partial tick (0-1) for smooth rendering
 */
abstract class Interpolatable {
    /**
     * Previous frame/tick state for interpolation.
     */
    protected val previousRenderState = RenderState()

    /**
     * Current state.
     */
    protected val currentRenderState = RenderState()

    /**
     * Initializes the initial state.
     * @param x Initial X coordinate
     * @param y Initial Y coordinate
     */
    protected open fun initRenderState(x: Float = 0f, y: Float = 0f) {
        currentRenderState.x = x
        currentRenderState.y = y
        previousRenderState.copyFrom(currentRenderState)
    }

    /**
     * Must be called at the end of each tick.
     * Copies current state to previous for interpolation.
     */
    fun updateRenderState() {
        previousRenderState.copyFrom(currentRenderState)
    }

    /**
     * Returns interpolated state between previous and current.
     * @param alpha Interpolation factor between 0 and 1 (typically metrics.timer.partialTick)
     * @return Interpolated state
     */
    fun getInterpolatedState(alpha: Float = RenderEngineCore.getCoreApi().metrics.timer.partialTick): RenderState {
        return RenderState.interpolate(previousRenderState, currentRenderState, alpha)
    }
}