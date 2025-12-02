package ua.terra.renderengine

import ua.terra.renderengine.text.fonts.FontRegistry
import ua.terra.renderengine.texture.registry.TextureRegistry
import ua.terra.renderengine.util.DEFAULT_ATLAS_PADDING
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
    abstract val resourcesPath: String
    val cachePath: String get() = "$resourcesPath/cache"

    val window = Window(windowName, windowWidth, windowHeight, windowX, windowY)
    var metrics = Metrics(tickRate)

    val textureRegistry = TextureRegistry()
    val fontRegistry = FontRegistry()

    val renderEngine = RenderEngine(textureRegistry)

    fun enable() {
        _INSTANCE = this
        window.open(this)
        onEnable()
        setupRenderEngines()
        window.startGameLoop(this)
    }

    open fun onEnable() {}
    abstract fun tick()

    abstract fun onRender()

    fun render() {
        onRender()
        renderEngine.flush()
    }

    open fun onDisable() {}

    internal fun disable() {
        onDisable()
    }

    open fun setupRenderEngines() {
        renderEngine.register()
    }

    fun reloadShaders() {
        renderEngine.reloadShader()
    }

    fun reloadTextures(cachePath: String, padding: Int = DEFAULT_ATLAS_PADDING) {
        textureRegistry.reload(cachePath, padding)
    }

    fun reloadFonts(cachePath: String) {
        fontRegistry.reload(cachePath)
    }

    companion object {
        private var _INSTANCE: RenderEngineCore? = null
        fun <C : RenderEngineCore> getApi(): C = (_INSTANCE as? C)
            ?: throw UninitializedPropertyAccessException("RenderEngineCore is not initialized!")
        fun getCoreApi(): RenderEngineCore = _INSTANCE
            ?: throw UninitializedPropertyAccessException("RenderEngineCore is not initialized!")
    }
}