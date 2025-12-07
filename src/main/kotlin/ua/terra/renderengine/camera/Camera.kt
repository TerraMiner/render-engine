package ua.terra.renderengine.camera

import ua.terra.renderengine.RenderEngineCore
import ua.terra.renderengine.render.Interpolatable
import ua.terra.renderengine.util.Cooldown
import ua.terra.renderengine.util.Point
import kotlin.math.abs
import kotlin.math.floor


/**
 * Abstract 2D camera system with smooth interpolation, zoom control, and boundary restrictions.
 * Extend this class to implement custom camera behavior for your game.
 *
 * Features:
 * - Smooth zoom in/out with configurable speed and limits
 * - Target following with customizable boundary logic
 * - Interpolated rendering for smooth movement between ticks
 * - Screen-to-scene coordinate conversion (override for custom behavior)
 *
 * @property core RenderEngineCore instance for accessing window and render engine
 */
abstract class Camera(protected val core: RenderEngineCore) : Interpolatable() {

    /**
     * Minimum allowed zoom value (1.0 = normal size).
     */
    open var minZoomValue = 1f

    /**
     * Maximum allowed zoom value (1.0 = normal size).
     */
    open var maxZoomValue = 2f

    /**
     * Amount of zoom change per zoomIn/zoomOut call.
     */
    open var zoomSpeed = 0.1f

    /**
     * Target object that camera follows. Set to null for manual camera control.
     */
    var cameraTarget: CameraTarget? = null

    /**
     * Cooldown timer to prevent too rapid zoom changes.
     */
    var zoomCooldown = Cooldown(50).apply { start() }

    /**
     * Optional boundary constraints for camera movement.
     * When set, camera will not move outside these bounds.
     */
    var bounds: CameraBounds? = null

    internal var targetZoomValue = 1.0f

    /**
     * Current camera X position (read-only, use cameraTarget or manual methods to modify).
     */
    val x: Float get() = currentRenderState.x

    /**
     * Current camera Y position (read-only, use cameraTarget or manual methods to modify).
     */
    val y: Float get() = currentRenderState.y

    /**
     * Increases zoom level by zoomSpeed.
     * Respects zoomCooldown to prevent too rapid changes.
     */
    fun zoomIn() {
        if (!zoomCooldown.isEnded()) return
        targetZoomValue = (targetZoomValue + zoomSpeed).coerceAtMost(maxZoomValue)
        zoomCooldown.start()
    }

    /**
     * Decreases zoom level by zoomSpeed.
     * Respects zoomCooldown to prevent too rapid changes.
     */
    fun zoomOut() {
        if (!zoomCooldown.isEnded()) return
        targetZoomValue = (targetZoomValue - zoomSpeed).coerceAtLeast(minZoomValue)
        zoomCooldown.start()
    }

    /**
     * Returns the current interpolated zoom level.
     * Override for custom zoom behavior.
     */
    open fun getCurrentZoom(): Float = currentRenderState.scaleX

    /**
     * Directly sets the current zoom level, bypassing smooth interpolation.
     * Override for custom zoom behavior.
     * @param value The new zoom level
     */
    open fun setCurrentZoom(value: Float) {
        currentRenderState.scaleX = value
        currentRenderState.scaleY = value
    }

    open fun setTargetZoomValue(value: Float) {
        targetZoomValue = value
    }

    /**
     * Initializes the camera to follow a specific target.
     * Should be called once during setup.
     * @param cameraTarget The target object to follow
     */
    open fun initialize(cameraTarget: CameraTarget) {
        this.cameraTarget = cameraTarget
        initRenderState()
        updateCameraPosition()
        previousRenderState.copyFrom(currentRenderState)
    }

    /**
     * Converts screen X coordinate to scene X coordinate using interpolated camera state.
     * @param screenX Screen X coordinate
     * @return Scene X coordinate
     */
    fun getScenePosX(screenX: Float): Float {
        val screenCenter = core.window.width / 2f
        val interpolated = getInterpolatedState()
        val interpolatedZoom = interpolated.scaleX
        val offsetFromCenter = (screenCenter - screenX) / interpolatedZoom - screenCenter
        return -interpolated.x - offsetFromCenter
    }

    /**
     * Converts screen X coordinate to scene X coordinate (integer variant).
     */
    fun getScenePosX(screenX: Int): Int = floor(getScenePosX(screenX.toFloat())).toInt()

    /**
     * Converts screen Y coordinate to scene Y coordinate using interpolated camera state.
     * @param screenY Screen Y coordinate
     * @return Scene Y coordinate
     */
    fun getScenePosY(screenY: Float): Float {
        val screenCenter = core.window.height / 2f
        val interpolated = getInterpolatedState()
        val interpolatedZoom = interpolated.scaleY
        val offsetFromCenter = (screenCenter - screenY) / interpolatedZoom - screenCenter
        return -interpolated.y - offsetFromCenter
    }

    /**
     * Converts screen Y coordinate to scene Y coordinate (integer variant).
     */
    fun getScenePosY(screenY: Int): Int = floor(getScenePosY(screenY.toFloat())).toInt()

    /**
     * Converts screen coordinates to scene coordinates.
     * @param screenX Screen X coordinate
     * @param screenY Screen Y coordinate
     * @return Scene coordinates as Point
     */
    fun getScenePos(screenX: Int, screenY: Int): Point<Float> {
        return Point(getScenePosX(screenX.toFloat()), getScenePosY(screenY.toFloat()))
    }

    /**
     * Updates camera position to follow target and applies smooth zoom interpolation.
     * Should be called every game tick.
     */
    fun tick() {
        updateRenderState()

        if (currentRenderState.scaleX != targetZoomValue) {
            val zoomDiff = targetZoomValue - currentRenderState.scaleX
            currentRenderState.scaleX += zoomDiff * 0.15f
            currentRenderState.scaleY = currentRenderState.scaleX

            if (abs(currentRenderState.scaleX - targetZoomValue) < 0.01f) {
                currentRenderState.scaleX = targetZoomValue
                currentRenderState.scaleY = targetZoomValue
            }
        }

        updateCameraPosition()
    }

    private fun updateCameraPosition() {
        val target = cameraTarget?.getCameraPoint() ?: return

        val targetX = target.x
        val targetY = target.y

        var desiredX = core.window.centerX - targetX
        var desiredY = core.window.centerY - targetY

        bounds?.let { b ->
            val halfVisibleWidth = (core.window.width / (2 * currentRenderState.scaleX))
            val halfVisibleHeight = (core.window.height / (2 * currentRenderState.scaleY))

            val minViewX = core.window.centerX - halfVisibleWidth
            val maxViewX = core.window.centerX - (b.maxX - halfVisibleWidth)

            val minViewY = core.window.centerY - halfVisibleHeight
            val maxViewY = core.window.centerY - (b.maxY - halfVisibleHeight)

            desiredX = if (maxViewX > minViewX) {
                core.window.centerX - b.maxX / 2f
            } else {
                desiredX.coerceIn(maxViewX, minViewX)
            }

            desiredY = if (maxViewY > minViewY) {
                core.window.centerY - b.maxY / 2f
            } else {
                desiredY.coerceIn(maxViewY, minViewY)
            }
        }

        currentRenderState.x = desiredX
        currentRenderState.y = desiredY
    }

    /**
     * Binds camera offset to the render engine.
     * Should be called before rendering to apply camera transformation.
     * Override to apply camera to additional renderers (e.g., lighting system).
     */
    open fun bind() {
        val interpolated = getInterpolatedState()
        val renderView = Point(interpolated.x, interpolated.y)
        core.renderEngine.setCameraOffset(renderView)
    }

    /**
     * Applies camera zoom to the render engine.
     * Should be called before rendering to apply zoom transformation.
     * Override to apply zoom to additional renderers (e.g., lighting system).
     */
    open fun applyZoom() {
        val interpolated = getInterpolatedState()
        val interpolatedZoom = interpolated.scaleX
        core.renderEngine.setCameraScale(interpolatedZoom)
    }
}
