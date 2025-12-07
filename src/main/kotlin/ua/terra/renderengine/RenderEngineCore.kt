package ua.terra.renderengine

import ua.terra.renderengine.input.Keyboard
import ua.terra.renderengine.input.Mouse
import ua.terra.renderengine.resource.ResourcePackManager
import ua.terra.renderengine.resource.ResourceProvider
import ua.terra.renderengine.text.fonts.FontRegistry
import ua.terra.renderengine.texture.registry.TextureRegistry
import ua.terra.renderengine.util.DEFAULT_ATLAS_PADDING
import ua.terra.renderengine.window.Metrics
import ua.terra.renderengine.window.Window

/**
 * Core render-engine class for managing window, rendering and input.
 *
 * Provides access to:
 * - Window management
 * - Texture rendering with atlas support
 * - Font rendering with TrueType/OpenType support
 * - Input handling (keyboard and mouse)
 * - 2D camera with zoom and interpolation
 * - Resource pack system
 */
abstract class RenderEngineCore(
    windowName: String,
    windowX: Int,
    windowY: Int,
    windowWidth: Int,
    windowHeight: Int,
    tickRate: Int,
) {
    /**
     * Resource pack manager for loading resources from packs or JAR.
     */
    val resourcePackManager = ResourcePackManager()

    /**
     * Path to cache directory for texture and font atlases.
     * By default uses ResourceProvider cache path.
     * Can be overridden in subclass for custom resource systems.
     */
    open val resourcesPath: String
        get() = resourcePackManager.getCachePath()

    val cachePath: String get() = "$resourcesPath/cache"

    /**
     * Application window. Manages OpenGL context and GLFW window.
     */
    val window = Window(windowName, windowWidth, windowHeight, windowX, windowY)

    /**
     * Application metrics (FPS, tick rate, partial tick for interpolation).
     */
    var metrics = Metrics(tickRate)

    /**
     * Texture registry for rendering.
     * Manages texture atlases and optimization.
     */
    val textureRegistry = TextureRegistry()

    /**
     * Font registry for text rendering.
     * Supports TrueType and OpenType fonts.
     */
    val fontRegistry = FontRegistry()

    /**
     * Main render engine for geometry and sprites.
     */
    val renderEngine = RenderEngine(textureRegistry)

    /**
     * Keyboard input manager.
     * Register key handlers in [onEnable].
     */
    val keyboard = Keyboard()

    /**
     * Mouse input manager.
     * Register click and movement handlers in [onEnable].
     */
    val mouse = Mouse()

    /**
     * Initializes the core rendering system.
     * @param title Window title
     * @param width Window width
     * @param height Window height
     * @param resourcePacks List of resource pack paths
     * @param enableVSync Whether to enable vertical synchronization
     */
    fun enable() {
        _INSTANCE = this
        ResourceProvider.register(resourcePackManager)
        window.open(this)
        setupInput()
        onEnable()
        setupRenderEngines()
        window.startGameLoop(this)
    }

    private fun setupInput() {
        keyboard.setup(window.id)
        mouse.setup(window.id)
    }

    /**
     * Called once during application initialization.
     * Use this method to set up resources and register input handlers.
     */
    open fun onEnable() {}

    /**
     * Called every tick (60 times per second by default).
     * Use this method to update application logic.
     */
    abstract fun tick()

    /**
     * Called every frame for rendering.
     * OpenGL context is active, ready for drawing.
     */
    abstract fun onRender()

    /**
     * Renders a single frame.
     * Calls onRender() and flushes the render engine.
     * This method is called automatically by the game loop.
     */
    fun render() {
        onRender()
        renderEngine.flush()
    }

    /**
     * Called before application shutdown.
     * Use this for resource cleanup.
     */
    open fun onDisable() {}

    internal fun disable() {
        onDisable()
    }

    /**
     * Render engine setup. Override if using custom shaders.
     */
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

    /**
     * Gets the core API instance.
     * @return The core API for advanced operations
     */
    companion object {
        private var _INSTANCE: RenderEngineCore? = null

        @Suppress("UNCHECKED_CAST")
        fun <C : RenderEngineCore> getApi(): C = (_INSTANCE as? C)
            ?: throw UninitializedPropertyAccessException("RenderEngineCore is not initialized!")

        fun getCoreApi(): RenderEngineCore = _INSTANCE
            ?: throw UninitializedPropertyAccessException("RenderEngineCore is not initialized!")
    }
}