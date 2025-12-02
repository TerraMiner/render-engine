package ua.terra.renderengine.window

import org.lwjgl.BufferUtils
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.glfw.GLFWWindowSizeCallback
import org.lwjgl.opengl.GL.createCapabilities
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil.NULL
import ua.terra.renderengine.RenderEngineCore
import ua.terra.renderengine.texture.manager.TextureManager
import ua.terra.renderengine.util.Color
import ua.terra.renderengine.util.DEFAULT_MAX_FPS
import ua.terra.renderengine.util.MIN_WINDOW_HEIGHT
import ua.terra.renderengine.util.MIN_WINDOW_WIDTH
import ua.terra.renderengine.util.Timer
import java.io.File
import kotlin.math.min

class Window(
    val name: String,
    var width: Int,
    var height: Int,
    initialX: Int = -1,
    initialY: Int = -1
) {
    var id: Long = 0
        private set

    var bgColor: Color = Color(150, 200, 250)
    var maxFpsLimit: Int = DEFAULT_MAX_FPS

    private val monitor = glfwGetPrimaryMonitor()
    private lateinit var videoMode: GLFWVidMode
    private var windowedWidth = width
    private var windowedHeight = height
    private var windowPosX = initialX
    private var windowPosY = initialY

    val centerX get() = width / 2f
    val centerY get() = height / 2f

    private var isCreated = false
    private var isShown = false

    var isFullScreen = false
        set(value) {
            if (field == value) return
            if (value) saveWindowedState()
            field = value
            applyWindowMode()
            updateViewport()
        }

    var vsync: Boolean = false
        set(value) {
            field = value
            glfwSwapInterval(if (value) 1 else 0)
        }

    var fpsLimit = maxFpsLimit
        get() = if (vsync) videoMode.refreshRate() else field
        set(value) {
            field = value.coerceIn(5, maxFpsLimit)
        }

    private var onResizeCallbacks = mutableListOf<(oldWidth: Int, oldHeight: Int, newWidth: Int, newHeight: Int) -> Unit>()

    fun onResize(callback: (oldWidth: Int, oldHeight: Int, newWidth: Int, newHeight: Int) -> Unit) {
        onResizeCallbacks.add(callback)
    }

    fun onResize(callback: () -> Unit) {
        onResizeCallbacks.add { _, _, _, _ -> callback() }
    }

    fun create() {
        if (isCreated) {
            println("Window already created!")
            return
        }

        setupErrorHandling()
        createWindowInternal()
        configureWindow()
        isCreated = true
    }

    fun show(core: RenderEngineCore) {
        if (!isCreated) {
            throw IllegalStateException("Cannot show window - not created yet!")
        }
        if (isShown) {
            println("Window already shown!")
            return
        }

        if (!isFullScreen) {
            centerWindow()
        }

        glfwShowWindow(id)
        glfwMakeContextCurrent(id)
        createCapabilities()

        setupRendering()
        setupVisuals(core.resourcesPath)

        isShown = true
    }

    fun hide() {
        if (!isShown) return
        glfwHideWindow(id)
        isShown = false
    }

    fun destroy() {
        if (!isCreated) return

        glfwFreeCallbacks(id)
        glfwSetErrorCallback(null)?.free()
        glfwDestroyWindow(id)

        isCreated = false
        isShown = false
    }

    fun open(core: RenderEngineCore) {
        create()
        configureCallbacks(core)
        show(core)
    }

    fun startGameLoop(core: RenderEngineCore) {
        if (!isCreated || !isShown) {
            throw IllegalStateException("Window must be created and shown before starting game loop!")
        }

        var lastFpsUpdateTime = System.nanoTime()
        var lastRenderTime = System.nanoTime()
        val metrics = core.metrics
        val timer = metrics.timer

        while (!glfwWindowShouldClose(id)) {
            val currentTime = System.currentTimeMillis()
            val currentNano = System.nanoTime()

            processInput(metrics)
            val ticksToRun = updateTicks(timer, currentTime)
            runTicks(core, ticksToRun, metrics)

            val shouldRender = checkRenderTime(currentNano, lastRenderTime)
            if (shouldRender) {
                renderFrame(core, metrics)
                lastRenderTime = currentNano
            }

            lastFpsUpdateTime = updateMetrics(currentNano,lastFpsUpdateTime, metrics)
        }

        close(core)
    }

    fun close(core: RenderEngineCore) {
        core.disable()
        destroy()
        glfwTerminate()
    }

    private fun setupErrorHandling() {
        glfwSetErrorCallback(object : GLFWErrorCallback() {
            override fun invoke(error: Int, description: Long) {
                throw IllegalStateException(getDescription(description))
            }
        })
        GLFWErrorCallback.createPrint(System.err).set()
    }

    private fun createWindowInternal() {
        val monitorHandle = if (isFullScreen) monitor else 0
        id = glfwCreateWindow(width, height, name, monitorHandle, 0)

        if (id == NULL) {
            throw RuntimeException("Failed to create the GLFW window")
        }
    }

    private fun configureWindow() {
        videoMode = glfwGetVideoMode(monitor)!!
    }

    private fun configureCallbacks(core: RenderEngineCore) {
        glfwSetWindowSizeLimits(id, MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT, GLFW_DONT_CARE, GLFW_DONT_CARE)

        glfwSetWindowSizeCallback(id, object : GLFWWindowSizeCallback() {
            override fun invoke(argWindow: Long, argWidth: Int, argHeight: Int) {
                val oldWidth = width
                val oldHeight = height
                width = argWidth
                height = argHeight
                updateViewport()
                onResizeCallbacks.forEach { it.invoke(oldWidth, oldHeight, width, height) }
            }
        })

        glfwSetWindowRefreshCallback(id) {
            glClear(GL_COLOR_BUFFER_BIT)
            core.render()
            glfwSwapBuffers(id)
        }
    }

    private fun setupRendering() {
        glEnable(GL_TEXTURE_2D)
        vsync = false
    }

    private fun setupVisuals(resourcesDir: String) {
        setWindowIcon(resourcesDir)
        setCursorIcon(resourcesDir)
    }

    private fun processInput(metrics: Metrics) {
        metrics.pollLag.measure(::glfwPollEvents)
    }

    private fun updateTicks(timer: Timer, currentTime: Long): Int {
        return timer.advanceTime(currentTime)
    }

    private fun runTicks(core: RenderEngineCore, ticksToRun: Int, metrics: Metrics) {
        repeat(min(10, ticksToRun)) {
            metrics.tpsLag.measure(core::tick)
            metrics.ticksPerSecond.increase()
            metrics.timer.tickCount++
        }
    }

    private fun checkRenderTime(currentNano: Long, lastRenderTime: Long): Boolean {
        val frameTime = 1_000_000_000L / fpsLimit
        return currentNano - lastRenderTime >= frameTime
    }

    private fun renderFrame(core: RenderEngineCore, metrics: Metrics) {
        metrics.videoLag.measure {
            glClear(GL_COLOR_BUFFER_BIT)
            core.render()
            glfwSwapBuffers(id)
        }
        metrics.framesPerSecond.increase()
    }

    private fun updateMetrics(
        currentNano: Long,
        lastFpsUpdateTime: Long,
        metrics: Metrics,
    ): Long {
        if (currentNano - lastFpsUpdateTime >= 1_000_000_000L) {
            metrics.framesPerSecond.flush()
            metrics.ticksPerSecond.flush()
            return currentNano
        }
        return lastFpsUpdateTime
    }


    private fun saveWindowedState() {
        val posXBuf = BufferUtils.createIntBuffer(1)
        val posYBuf = BufferUtils.createIntBuffer(1)
        glfwGetWindowPos(id, posXBuf, posYBuf)
        windowPosX = posXBuf.get()
        windowPosY = posYBuf.get()
        windowedWidth = width
        windowedHeight = height
    }

    private fun applyWindowMode() {
        if (isFullScreen) {
            glfwSetWindowMonitor(
                id, monitor, 0, 0,
                videoMode.width(), videoMode.height(),
                videoMode.refreshRate()
            )
        } else {
            glfwSetWindowMonitor(
                id, 0, windowPosX, windowPosY,
                windowedWidth, windowedHeight,
                GLFW_DONT_CARE
            )
        }
    }

    private fun updateViewport() {
        val viewportWidth = if (isFullScreen) videoMode.width() else width
        val viewportHeight = if (isFullScreen) videoMode.height() else height
        glViewport(0, 0, viewportWidth, viewportHeight)
    }

    private fun centerWindow() {
        val targetX = if (windowPosX != -1) windowPosX else (videoMode.width() - width) / 2
        val targetY = if (windowPosY != -1) windowPosY else (videoMode.height() - height) / 2
        glfwSetWindowPos(id, targetX, targetY)
    }

    private fun setCursorIcon(resourcesDir: String) {
        val image = GLFWImage.create()
        val cursorPath = File(resourcesDir, "cursor/cursor.png").absolutePath
        val data = TextureManager.loadToGLFWImage(cursorPath, image, 4)
        val cursor = glfwCreateCursor(image, 0, 0)
        glfwSetCursor(id, cursor)
        data.free()
    }

    private fun setWindowIcon(resourcesDir: String, vararg iconSizes: Int = intArrayOf(16, 32, 48, 64)) {
        val icons = GLFWImage.malloc(iconSizes.size)

        val imageDatas = iconSizes.mapIndexed { index, size ->
            val iconPath = File(resourcesDir, "icons/icon_${size}x${size}.png").absolutePath
            TextureManager.loadToGLFWImageBuffer(
                iconPath,
                index,
                icons,
                4
            )
        }

        glfwSetWindowIcon(id, icons)

        imageDatas.forEach { it.free() }
        icons.free()
    }
}