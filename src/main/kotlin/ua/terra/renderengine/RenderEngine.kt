package ua.terra.renderengine

import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.*
import org.lwjgl.opengl.GL31.glDrawArraysInstanced
import org.lwjgl.opengl.GL33.glVertexAttribDivisor
import ua.terra.renderengine.renderer.GeometryRenderer
import ua.terra.renderengine.renderer.TextRenderer
import ua.terra.renderengine.renderer.TextureRenderer
import ua.terra.renderengine.shader.BatchShader
import ua.terra.renderengine.texture.manager.TextureManager
import ua.terra.renderengine.texture.registry.TextureRegistry
import ua.terra.renderengine.util.*
import java.util.*

/**
 * Main entry point for the render engine.
 * Provides high-level rendering API for textures, geometry, and text.
 */
class RenderEngine(
    private val textureRegistry: TextureRegistry
) {
    private val maxSprites = MAX_SPRITES
    private val floatsPerSprite = FLOATS_PER_SPRITE
    private val floatsPerSpriteBytes = FLOATS_PER_SPRITE_BYTES
    private val bufferSize = BUFFER_SIZE
    private val bufferSizeBytes = BUFFER_SIZE_BYTES

    private lateinit var shader: BatchShader
    private var vao = 0
    private var vbo = 0

    private var cameraScale = 1.0f
    private var cameraX = 0f
    private var cameraY = 0f

    private val tempBufferStore = FloatArray(floatsPerSprite)

    private val commandPool = Array(maxSprites) { RenderCommand.TextureCommand() }
    private var commandCount = 0

    private val bufferData = BufferUtils.createFloatBuffer(bufferSize)


    private var currentScissor: ScissorRect? = null

    private var startTimeMillis = System.currentTimeMillis()

    private val zIndexComparator = Comparator<RenderCommand> { a, b ->
        val zCompare = a.zI - b.zI
        if (zCompare != 0) return@Comparator zCompare
        a.tId - b.tId
    }

    val textureRenderer = TextureRenderer(this)
    val geometryRenderer = GeometryRenderer(this, textureRegistry)
    val textRenderer = TextRenderer(this)

    /**
     * Initializes the render engine.
     * Must be called before using any rendering functions.
     */
    fun register() {
        shader = BatchShader()
        vao = glGenVertexArrays()
        vbo = glGenBuffers()

        shader.register()
        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, bufferSizeBytes.toLong(), GL_STREAM_DRAW)

        var offsetBytes = 0L

        fun addAttrib(index: Int, componentCount: Int) {
            glVertexAttribPointer(index, componentCount, GL_FLOAT, false, floatsPerSpriteBytes, offsetBytes)
            glEnableVertexAttribArray(index)
            glVertexAttribDivisor(index, 1)
            offsetBytes += componentCount * Float.SIZE_BYTES
        }

        fun addIntAttrib(index: Int, componentCount: Int) {
            glVertexAttribIPointer(index, componentCount, GL_UNSIGNED_INT, floatsPerSpriteBytes, offsetBytes)
            glEnableVertexAttribArray(index)
            glVertexAttribDivisor(index, 1)
            offsetBytes += componentCount * Int.SIZE_BYTES
        }

        addAttrib(0, 4)
        addAttrib(1, 4)
        addIntAttrib(2, 1)
        addAttrib(3, 2)
        addAttrib(4, 1)
        addAttrib(5, 1)
        addAttrib(6, 1)
        addAttrib(7, 4)
        addAttrib(8, 1)

        glBindBuffer(GL_ARRAY_BUFFER, 0)

        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        val window = RenderEngineCore.getCoreApi().window
        val c = window.bgColor
        glClearColor(c.r, c.g, c.b, c.a)
        window.onResize { updateProjection() }
    }

    /**
     * Reloads the shader program and updates the projection matrix.
     */
    fun reloadShader() {
        shader.reload()
        updateProjection()
        setCameraScale(cameraScale)
        setCameraOffset(Point(cameraX, cameraY))
    }

    /**
     * Updates the projection matrix based on the current window size.
     */
    fun updateProjection() {
        val window = RenderEngineCore.getCoreApi().window
        shader.bind()
        shader.setProjection(Matrix4f().setOrtho2D(0f, window.width.toFloat(), window.height.toFloat(), 0f))
        shader.setCameraCenter(window.centerX, window.centerY)
    }

    /**
     * Sets the camera scale.
     * @param scale The new scale for the camera
     */
    fun setCameraScale(scale: Float) {
        cameraScale = scale
        shader.bind()
        shader.setCameraScale(cameraScale, cameraScale)
    }

    /**
     * Sets the camera offset.
     * @param offset The new offset for the camera
     */
    fun setCameraOffset(offset: Point<Float>) {
        cameraX = -offset.x
        cameraY = -offset.y
        shader.bind()
        shader.setCameraOffset(offset.x, offset.y)
    }

    private fun renderAllBatches(): Int {
        if (commandCount == 0) return 0

        var drawCalls = 0
        var currentTexture = commandPool[0].tId
        var batchStart = 0

        for (i in 1 until commandCount) {
            val cmd = commandPool[i]
            if (cmd.tId != currentTexture) {
                renderBatch(currentTexture, batchStart, i)
                drawCalls++
                currentTexture = cmd.tId
                batchStart = i
            }
        }

        renderBatch(currentTexture, batchStart, commandCount)
        drawCalls++

        return drawCalls
    }

    private fun renderBatch(textureId: Int, start: Int, end: Int) {
        shader.bind()
        glBindVertexArray(vao)
        TextureManager.bindTex(textureId)

        bufferData.clear()

        for (i in start until end) {
            val cmd = commandPool[i]
            tempBufferStore[0] = cmd.x
            tempBufferStore[1] = cmd.y
            tempBufferStore[2] = cmd.w
            tempBufferStore[3] = cmd.h
            tempBufferStore[4] = cmd.minU
            tempBufferStore[5] = cmd.minV
            tempBufferStore[6] = cmd.maxU
            tempBufferStore[7] = cmd.maxV
            tempBufferStore[8] = Float.fromBits(cmd.packedColor)
            tempBufferStore[9] = cmd.t
            tempBufferStore[10] = cmd.rot
            tempBufferStore[11] = cmd.iZ
            tempBufferStore[12] = cmd.iC
            tempBufferStore[13] = cmd.rT
            tempBufferStore[14] = cmd.sX
            tempBufferStore[15] = cmd.sY
            tempBufferStore[16] = cmd.sW
            tempBufferStore[17] = cmd.sH
            tempBufferStore[18] = cmd.tE
            bufferData.put(tempBufferStore)
        }

        bufferData.flip()
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferSubData(GL_ARRAY_BUFFER, 0, bufferData)
        glDrawArraysInstanced(GL_TRIANGLES, 0, 6, end - start)
    }

    /**
     * Submits a command for rendering a texture.
     * @param texture The texture ID
     * @param x X-coordinate
     * @param y Y-coordinate
     * @param width Width of the texture
     * @param height Height of the texture
     * @param minU U-coordinate of the texture
     * @param minV V-coordinate of the texture
     * @param maxU Max U-coordinate of the texture
     * @param maxV Max V-coordinate of the texture
     * @param zIndex Z-index for rendering order
     * @param color Tint color (RGBA packed as int)
     * @param thickness Line thickness (for rectangles)
     * @param rotation Rotation angle (for rectangles)
     * @param ignoreZoom Whether to ignore camera zoom
     * @param ignoreCamera Whether to ignore camera position
     * @param renderType Render type identifier
     * @param textEffect Text effect identifier (default is 0)
     */
    internal fun submitCommand(
        texture: Int,
        x: Float, y: Float, width: Float, height: Float,
        minU: Float, minV: Float, maxU: Float, maxV: Float,
        zIndex: Int,
        color: Int = -1,
        thickness: Float, rotation: Float,
        ignoreZoom: Boolean,
        ignoreCamera: Boolean,
        renderType: Float,
        textEffect: Float = 0f
    ) {
        if (commandCount >= maxSprites) {
            flush()
        }

        val cmd = commandPool[commandCount]
        cmd.tId = texture
        cmd.x = x
        cmd.y = y
        cmd.w = width
        cmd.h = height
        cmd.minU = minU
        cmd.minV = minV
        cmd.maxU = maxU
        cmd.maxV = maxV
        cmd.packedColor = color
        cmd.t = thickness
        cmd.rot = rotation
        cmd.iZ = if (ignoreZoom) 1f else 0f
        cmd.iC = if (ignoreCamera) 1f else 0f
        cmd.rT = renderType
        cmd.zI = zIndex

        if (currentScissor != null) {
            cmd.sX = currentScissor!!.x
            cmd.sY = currentScissor!!.y
            cmd.sW = currentScissor!!.width
            cmd.sH = currentScissor!!.height
        } else {
            cmd.sX = -1f
        }

        cmd.tE = textEffect
        commandCount++
    }

    /**
     * Flushes the render queue and executes the batched rendering.
     */
    fun flush() {
        if (commandCount == 0) return

        val metrics = RenderEngineCore.getCoreApi().metrics

        metrics.sortTime.measure {
            Arrays.sort(commandPool, 0, commandCount, zIndexComparator)
        }
        metrics.totalCommandCount = commandCount

        val currentTime = (System.currentTimeMillis() - startTimeMillis) / 1000.0f
        shader.bind()
        shader.setTime(currentTime)

        metrics.engineRenderTime.measure {
            metrics.drawCalls = renderAllBatches()
        }

        commandCount = 0
    }

    /**
     * Sets the scissor rectangle for clipping rendered content.
     * @param x X-coordinate of the scissor rectangle
     * @param y Y-coordinate of the scissor rectangle
     * @param width Width of the scissor rectangle
     * @param height Height of the scissor rectangle
     */
    fun setScissorRect(x: Float, y: Float, width: Float, height: Float) {
        currentScissor = ScissorRect(x, y, width, height)
    }

    /**
     * Clears the current scissor rectangle, disabling scissor testing.
     */
    fun clearScissorRect() {
        currentScissor = null
    }
}