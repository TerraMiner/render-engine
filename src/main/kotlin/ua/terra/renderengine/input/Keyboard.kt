package ua.terra.renderengine.input

import it.unimi.dsi.fastutil.ints.Int2BooleanOpenHashMap
import org.lwjgl.glfw.GLFW.*

/**
 * Keyboard input manager.
 * Provides methods to check key states and register key event callbacks.
 *
 * Supports three event types:
 * - onKeyPress: fires once when key is pressed
 * - onKeyPressed: fires every frame while key is held
 * - onKeyRelease: fires once when key is released
 */
class Keyboard {
    private val keyReleaseActions = mutableMapOf<Int, MutableList<() -> Unit>>()
    private val keyPressActions = mutableMapOf<Int, MutableList<() -> Unit>>()
    private val keyPressedActions = mutableMapOf<Int, MutableList<() -> Unit>>()
    private val pressedKeys = Int2BooleanOpenHashMap()
    private val charInputListeners = mutableListOf<(Char) -> Unit>()

    internal fun setup(windowId: Long) {
        glfwSetInputMode(windowId, GLFW_STICKY_KEYS, GLFW_TRUE)
        glfwSetKeyCallback(windowId, ::keyCallback)
        glfwSetCharCallback(windowId, ::charCallback)
    }

    private fun keyCallback(window: Long, key: Int, scancode: Int, action: Int, mods: Int) {
        when (action) {
            GLFW_PRESS -> {
                pressedKeys[key] = true
                keyPressActions[key]?.forEach { it() }
            }
            GLFW_RELEASE -> {
                pressedKeys[key] = false
                keyReleaseActions[key]?.forEach { it() }
            }
        }
    }

    private fun charCallback(window: Long, codepoint: Int) {
        val char = codepoint.toChar()
        charInputListeners.forEach { it(char) }
    }

    /**
     * Checks if key is currently pressed.
     * @param key The GLFW key code
     * @return true if the key is pressed
     */
    fun isKeyPressed(key: Int): Boolean = pressedKeys[key]

    /**
     * Checks if a key was just pressed this frame.
     * @param key The GLFW key code
     * @return true if the key was just pressed
     */
    fun isKeyJustPressed(key: Int): Boolean = TODO("Not implemented yet")

    /**
     * Checks if a key was just released this frame.
     * @param key The GLFW key code
     * @return true if the key was just released
     */
    fun isKeyJustReleased(key: Int): Boolean = TODO("Not implemented yet")

    /**
     * Registers key press handler (fires once).
     */
    fun onKeyPress(key: Int, action: () -> Unit) {
        keyPressActions.getOrPut(key) { mutableListOf() }.add(action)
    }

    /**
     * Registers key held handler (fires every frame).
     * Call [update] in game loop for this handler to work.
     */
    fun onKeyPressed(key: Int, action: () -> Unit) {
        keyPressedActions.getOrPut(key) { mutableListOf() }.add(action)
    }

    /**
     * Registers key release handler (fires once).
     */
    fun onKeyRelease(key: Int, action: () -> Unit) {
        keyReleaseActions.getOrPut(key) { mutableListOf() }.add(action)
    }

    /**
     * Registers text input handler.
     */
    fun onCharInput(action: (Char) -> Unit) {
        charInputListeners.add(action)
    }

    /**
     * Must be called every frame (in onRender method).
     * Processes onKeyPressed events.
     */
    fun update() {
        keyPressedActions.forEach { (key, actions) ->
            if (isKeyPressed(key)) actions.forEach { it() }
        }
    }

    /**
     * Clears all registered handlers.
     */
    fun clearActions() {
        keyPressActions.clear()
        keyPressedActions.clear()
        keyReleaseActions.clear()
        charInputListeners.clear()
    }
}
