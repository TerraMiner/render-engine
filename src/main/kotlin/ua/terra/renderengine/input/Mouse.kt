package ua.terra.renderengine.input

import org.lwjgl.glfw.GLFW.*

/**
 * Mouse input manager.
 *
 * Supports events:
 * - onMouseClick: fires once on button click
 * - onMousePressed: fires every frame while button is held
 * - onMouseRelease: fires once on button release
 * - onMouseScroll: fires on scroll wheel movement
 * - onMouseMove: fires on mouse movement
 */
class Mouse {
    private val pressedButtons = BooleanArray(8)

    private val clickActions = mutableMapOf<Int, MutableList<(Int, Int) -> Unit>>()
    private val releaseActions = mutableMapOf<Int, MutableList<(Int, Int) -> Unit>>()
    private val pressedActions = mutableMapOf<Int, MutableList<(Int, Int, Int, Int) -> Unit>>()
    private val scrollActions = mutableListOf<(Double) -> Unit>()
    private val moveActions = mutableListOf<(Int, Int, Int, Int) -> Unit>()

    private var xPos: Int = 0
    private var yPos: Int = 0
    private var lastX: Int = 0
    private var lastY: Int = 0

    /**
     * Current mouse X position on screen.
     */
    val x: Float get() = xPos.toFloat()

    /**
     * Current mouse Y position on screen.
     */
    val y: Float get() = yPos.toFloat()

    /**
     * Previous frame mouse X position.
     */
    val lastXPos: Float get() = lastX.toFloat()

    /**
     * Previous frame mouse Y position.
     */
    val lastYPos: Float get() = lastY.toFloat()

    internal fun setup(windowId: Long) {
        glfwSetInputMode(windowId, GLFW_STICKY_MOUSE_BUTTONS, GLFW_TRUE)
        glfwSetCursorPosCallback(windowId, ::mousePosCallback)
        glfwSetMouseButtonCallback(windowId, ::mouseButtonCallback)
        glfwSetScrollCallback(windowId, ::mouseScrollCallback)
    }

    /**
     * Must be called every frame (in onRender method).
     * Processes onMousePressed events.
     */
    fun update() {
        for (button in pressedButtons.indices) {
            if (pressedButtons[button]) {
                pressedActions[button]?.forEach { it(xPos, yPos, lastX, lastY) }
            }
        }
    }

    private fun mousePosCallback(window: Long, xps: Double, yps: Double) {
        lastX = xPos
        lastY = yPos
        xPos = xps.toInt()
        yPos = yps.toInt()
        moveActions.forEach { it(xPos, yPos, lastX, lastY) }
    }

    private fun mouseButtonCallback(window: Long, button: Int, action: Int, mods: Int) {
        when (action) {
            GLFW_PRESS -> {
                pressedButtons[button] = true
                clickActions[button]?.forEach { it(xPos, yPos) }
            }
            GLFW_RELEASE -> {
                pressedButtons[button] = false
                releaseActions[button]?.forEach { it(xPos, yPos) }
            }
        }
    }

    private fun mouseScrollCallback(window: Long, xOffset: Double, yOffset: Double) {
        if (yOffset != 0.0) {
            scrollActions.forEach { it(yOffset) }
        }
    }

    /**
     * Registers click handler (fires once).
     * @param button Mouse button code (GLFW_MOUSE_BUTTON_LEFT, GLFW_MOUSE_BUTTON_RIGHT, etc.)
     * @param action Function receives click coordinates (x, y)
     */
    fun onMouseClick(button: Int, action: (Int, Int) -> Unit) {
        clickActions.getOrPut(button) { mutableListOf() }.add(action)
    }

    /**
     * Registers release handler (fires once).
     */
    fun onMouseRelease(button: Int, action: (Int, Int) -> Unit) {
        releaseActions.getOrPut(button) { mutableListOf() }.add(action)
    }

    /**
     * Registers scroll handler.
     * @param action Function receives offset (positive for scroll up)
     */
    fun onMouseScroll(action: (Double) -> Unit) {
        scrollActions.add(action)
    }

    /**
     * Registers button hold handler (fires every frame).
     * Call [update] in game loop for this handler to work.
     * @param action Function receives current coordinates (x, y) and previous frame coordinates (lastX, lastY)
     */
    fun onMousePressed(button: Int, action: (Int, Int, Int, Int) -> Unit) {
        pressedActions.getOrPut(button) { mutableListOf() }.add(action)
    }

    /**
     * Registers mouse movement handler.
     */
    fun onMouseMove(action: (Int, Int, Int, Int) -> Unit) {
        moveActions.add(action)
    }

    /**
     * Checks if button is currently pressed.
     */
    fun isButtonPressed(button: Int): Boolean = pressedButtons.getOrElse(button) { false }

    /**
     * Clears all registered handlers.
     */
    fun clearActions() {
        clickActions.clear()
        releaseActions.clear()
        pressedActions.clear()
        scrollActions.clear()
        moveActions.clear()
    }
}
