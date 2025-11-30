package ua.terra.renderengine

import ua.terra.renderengine.util.Point
import ua.terra.renderengine.window.Metrics
import ua.terra.renderengine.window.Window

abstract class RenderEngineCore(
    windowName: String,
    windowX: Int,
    windowY: Int,
    windowWidth: Int,
    windowHeight: Int,
    tickRate: Int,
) {
    val window = Window(windowName, windowWidth, windowHeight, Point(windowX, windowY))
    val metrics = Metrics(tickRate)

    val renderEngine = RenderEngine(window)

    fun enable() {
        window.open(this)
        onEnable()
        window.startGameLoop(this)
    }

    open fun onEnable() {}
    abstract fun tick()
    abstract fun render()
    open fun onDisable() {}

    internal fun disable() {
        onDisable()
    }
}