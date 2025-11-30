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
import ua.terra.renderengine.texture.TextureManager
import ua.terra.renderengine.util.Color
import ua.terra.renderengine.util.Point
import kotlin.math.min

class Window(
    val name: String,
    var width: Int,
    var height: Int,
    val windowPosition: Point<Int> = Point(-1,-1)
) {
    var id: Long = 0

    var bgColor: Color = Color(150, 200, 250)
    var maxFpsLimit: Int = 240

    private val monitor = glfwGetPrimaryMonitor()
    private lateinit var videoMode: GLFWVidMode
    private var windowedWidth = width
    private var windowedHeight = height

    val centerX get() = width / 2f
    val centerY get() = height / 2f

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

    private var framesMetric: Int = 0
    private var ticksMetric: Int = 0
    private var resizeCallback: (Int, Int ,Int, Int) -> Unit = { oldWidth: Int, oldHeight: Int, newWidth: Int, newHeight: Int -> }

    fun open(core: RenderEngineCore) {
        setupErrorHandling()
        createWindow()
        configureWindow(core)
        setupRendering()
        setupVisuals()
    }

    fun startGameLoop(core: RenderEngineCore) {
        var lastFpsUpdateTime = System.nanoTime()
        var lastRenderTime = System.nanoTime()
        val metrics = core.metrics
        val timer = metrics.timer

        while (!glfwWindowShouldClose(id)) {
            val currentTime = System.currentTimeMillis()
            val currentNano = System.nanoTime()

            glfwPollEvents()
            metrics.pollLag = (System.nanoTime() - currentNano) / 1_000_000.0

            val ticksToRun = timer.advanceTime(currentTime)
            for (i in 0 until min(10, ticksToRun)) {
                val tickStart = System.nanoTime()
                core.tick()
                val tickEnd = System.nanoTime()
                metrics.tpsLag = (tickEnd - tickStart) / 1_000_000.0
                ticksMetric++
            }

            val frameTime = 1_000_000_000L / fpsLimit
            if (currentNano - lastRenderTime >= frameTime) {

                glClear(GL_COLOR_BUFFER_BIT)
                core.render()
                glfwSwapBuffers(id)

                val renderEnd = System.nanoTime()
                metrics.videoLag = (renderEnd - currentNano) / 1_000_000.0

                framesMetric++
                lastRenderTime = currentNano
            }

            if (currentNano - lastFpsUpdateTime >= 1_000_000_000L) {
                metrics.framesPerSecond = framesMetric
                metrics.ticksPerSecond = ticksMetric
                framesMetric = 0
                ticksMetric = 0
                lastFpsUpdateTime = currentNano
            }
        }

        close(core)
    }

    fun close(core: RenderEngineCore) {
        core.disable()
        glfwFreeCallbacks(id)
        glfwSetErrorCallback(null)?.free()
        glfwSetWindowShouldClose(id, true)
        glfwTerminate()
        glfwDestroyWindow(id)
    }

    private fun setupErrorHandling() {
        glfwSetErrorCallback(object : GLFWErrorCallback() {
            override fun invoke(error: Int, description: Long) {
                throw IllegalStateException(getDescription(description))
            }
        })
        GLFWErrorCallback.createPrint(System.err).set()
    }

    private fun createWindow() {
        val monitorHandle = if (isFullScreen) monitor else 0
        id = glfwCreateWindow(width, height, name, monitorHandle, 0)

        if (id == NULL) {
            throw RuntimeException("Failed to create the GLFW window")
        }
    }

    private fun configureWindow(core: RenderEngineCore) {
        videoMode = glfwGetVideoMode(monitor)!!

        if (!isFullScreen) {
            centerWindow()
        }

        glfwShowWindow(id)
        glfwMakeContextCurrent(id)
        createCapabilities()
        setupCallbacks(core)
    }

    private fun setupRendering() {
        glEnable(GL_TEXTURE_2D)
        vsync = false
    }

    private fun setupVisuals() {
        setWindowIcon()
        setCursorIcon()
    }

    private fun saveWindowedState() {
        val posXBuf = BufferUtils.createIntBuffer(1)
        val posYBuf = BufferUtils.createIntBuffer(1)
        glfwGetWindowPos(id, posXBuf, posYBuf)
        windowPosition.x = posXBuf.get()
        windowPosition.y = posYBuf.get()
        windowedWidth = width
        windowedHeight = height
    }

    private fun applyWindowMode() {
        glfwSetWindowMonitor(
            id,
            if (isFullScreen) monitor else 0,
            if (isFullScreen) 0 else windowPosition.x,
            if (isFullScreen) 0 else windowPosition.y,
            if (isFullScreen) videoMode.width() else windowedWidth,
            if (isFullScreen) videoMode.height() else windowedHeight,
            if (isFullScreen) videoMode.refreshRate() else GLFW_DONT_CARE
        )
    }

    private fun updateViewport() {
        val viewportWidth = if (isFullScreen) videoMode.width() else width
        val viewportHeight = if (isFullScreen) videoMode.height() else height

        glViewport(0, 0, viewportWidth, viewportHeight)
    }

    private fun centerWindow() {
        val targetX = if (windowPosition.x != -1) windowPosition.x else (videoMode.width() - width) / 2
        val targetY = if (windowPosition.y != -1) windowPosition.y else (videoMode.height() - height) / 2
        glfwSetWindowPos(id, targetX, targetY)
    }

    private fun setupCallbacks(core: RenderEngineCore) {
        glfwSetWindowSizeLimits(id, 800, 600, GLFW_DONT_CARE, GLFW_DONT_CARE)

        glfwSetWindowSizeCallback(id, object : GLFWWindowSizeCallback() {
            override fun invoke(argWindow: Long, argWidth: Int, argHeight: Int) {
                val oldWidth = width
                val oldHeight = height
                width = argWidth
                height = argHeight
                updateViewport()
                resizeCallback(oldWidth, oldHeight, width, height)
            }
        })

        glfwSetWindowRefreshCallback(id) {
            glClear(GL_COLOR_BUFFER_BIT)
            core.render()
            glfwSwapBuffers(id)
        }
    }

    private fun setCursorIcon() {
        val image = GLFWImage.create()
        val data = TextureManager.loadToGLFWImage("cursor/cursor.png", image, 4)
        val cursor = glfwCreateCursor(image, 0, 0)
        glfwSetCursor(id, cursor)
        data.free()
    }

    private fun setWindowIcon() {
        val iconSizes = intArrayOf(16, 32, 48, 64)
        val icons = GLFWImage.malloc(iconSizes.size)

        val imageDatas = iconSizes.mapIndexed { index, size ->
            TextureManager.loadToGLFWImageBuffer(
                "icons/icon_${size}x${size}.png",
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