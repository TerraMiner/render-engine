package ua.terra.renderengine

import ua.terra.renderengine.shader.BatchShader
import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.GL_BLEND
import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA
import org.lwjgl.opengl.GL11.GL_SRC_ALPHA
import org.lwjgl.opengl.GL11.GL_TRIANGLES
import org.lwjgl.opengl.GL11.GL_UNSIGNED_INT
import org.lwjgl.opengl.GL11.glBlendFunc
import org.lwjgl.opengl.GL11.glClearColor
import org.lwjgl.opengl.GL11.glEnable
import org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER
import org.lwjgl.opengl.GL15.GL_STREAM_DRAW
import org.lwjgl.opengl.GL15.glBindBuffer
import org.lwjgl.opengl.GL15.glBufferData
import org.lwjgl.opengl.GL15.glBufferSubData
import org.lwjgl.opengl.GL15.glGenBuffers
import org.lwjgl.opengl.GL20.glEnableVertexAttribArray
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL30.glBindVertexArray
import org.lwjgl.opengl.GL30.glGenVertexArrays
import org.lwjgl.opengl.GL30.glVertexAttribIPointer
import org.lwjgl.opengl.GL31.glDrawArraysInstanced
import org.lwjgl.opengl.GL33.glVertexAttribDivisor
import java.util.Arrays
import kotlin.math.round
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import ua.terra.renderengine.text.TextEffect
import ua.terra.renderengine.text.TextHorAlignment
import ua.terra.renderengine.text.TextHorPosition
import ua.terra.renderengine.text.TextVerAlignment
import ua.terra.renderengine.text.TextVerPosition
import ua.terra.renderengine.text.fonts.Fonts
import ua.terra.renderengine.texture.Texture
import ua.terra.renderengine.texture.TextureManager
import ua.terra.renderengine.texture.Textures
import ua.terra.renderengine.texture.model.Model
import ua.terra.renderengine.util.Point
import ua.terra.renderengine.window.Window
import java.util.*

class RenderEngine(val window: Window) {

    private var sortTime = 0L
    private var renderTime = 0L
    private var totalCommandCount = 0
    private var offsetBytes = 0L

    private val maxSprites = 128000
    private val floatsPerSprite = 19  // Изменено с 22 на 19
    private val floatsPerSpriteBytes = floatsPerSprite * Float.SIZE_BYTES
    private val bufferSize = maxSprites * floatsPerSprite
    private val bufferSizeBytes = bufferSize * Float.SIZE_BYTES

    private lateinit var shader: BatchShader
    private var vao = 0
    private var vbo = 0

    private var cameraScale = 1.0f
    private var cameraX = 0f
    private var cameraY = 0f

    private val tempBufferStore = FloatArray(floatsPerSprite)

    private val allCommands = arrayOfNulls<RenderCommand>(maxSprites)
    private var commandCount = 0

    private val bufferData = BufferUtils.createFloatBuffer(bufferSize)
    private var drawCalls = 0

    data class ScissorRect(val x: Float, val y: Float, val width: Float, val height: Float)
    private var currentScissor: ScissorRect? = null

    private var startTimeMillis = System.currentTimeMillis()

    private val commandPool = Array(maxSprites) { RenderCommand() }

    private val zIndexComparator = Comparator<RenderCommand> { a, b ->
        val zCompare = a.zI - b.zI
        if (zCompare != 0) return@Comparator zCompare
        a.tId - b.tId
    }

    fun setScissorRect(x: Float, y: Float, width: Float, height: Float) {
        currentScissor = ScissorRect(x, y, width, height)
    }

    fun clearScissorRect() {
        currentScissor = null
    }

    fun render(
        texture: Texture,
        model: Model,
        x: Float, y: Float,
        width: Float, height: Float,
        zIndex: Int = 0,
        angle: Float = 0f,
        color: Int,
        thickness: Float = 1f,
        ignoreZoom: Boolean = true,
        ignoreCamera: Boolean = false
    ) {
        render(
            texture.id,
            x, y, width, height,
            model.uvX, model.uvY, model.uvMX, model.uvMY,
            zIndex,
            color,
            thickness, angle,
            ignoreZoom, ignoreCamera,
            RenderType.TEXTURE.value
        )
    }

    fun render(
        texture: Int,
        model: Model,
        x: Float, y: Float,
        width: Float, height: Float,
        zIndex: Int = 0,
        angle: Float = 0f,
        color: Int = -1,
        thickness: Float = 1f,
        ignoreZoom: Boolean = true,
        ignoreCamera: Boolean = false
    ) {
        render(
            texture,
            x, y, width, height,
            model.uvX, model.uvY, model.uvMX, model.uvMY,
            zIndex,
            color,
            thickness, angle,
            ignoreZoom, ignoreCamera,
            RenderType.TEXTURE.value
        )
    }

    fun renderText(
        text: String,
        x: Float, y: Float,
        fontSize: Int,
        zIndex: Int = 0,
        horAlign: TextHorAlignment = TextHorAlignment.LEFT,
        horPos: TextHorPosition = TextHorPosition.LEFT,
        verAlign: TextVerAlignment = TextVerAlignment.BOTTOM,
        verPos: TextVerPosition = TextVerPosition.BOTTOM,
        color: Int = -1,
        angle: Float = 0f,
        outlineThickness: Float = 6f,
        ignoreZoom: Boolean = true,
        ignoreCamera: Boolean = false,
        font: Fonts = Fonts.ANDYBOLD,
        effect: TextEffect = TextEffect.DEFAULT
    ) {
        val scale = fontSize.toFloat() / font.size
        if (text.isBlank()) return

        var lineCount = 1
        var maxWidth = 0
        var currentLineWidth = 0

        for (char in text) {
            if (char == '\n') {
                lineCount++
                if (currentLineWidth > maxWidth) maxWidth = currentLineWidth
                currentLineWidth = 0
            } else {
                currentLineWidth += font.getCharacter(char).advanceWidth
            }
        }
        if (currentLineWidth > maxWidth) maxWidth = currentLineWidth

        val maxWidthScaled = maxWidth * scale
        val maxHeight = lineCount * font.font.lineHeight * scale

        val startX = x - round(maxWidthScaled * horPos.offset)
        val startY = y - round(maxHeight * verPos.offset)

        var lineIndex = 0
        var lineStartIdx = 0

        for (i in text.indices) {
            if (text[i] == '\n' || i == text.length - 1) {
                val lineEnd = if (text[i] == '\n') i else i + 1
                val line = text.substring(lineStartIdx, lineEnd)

                val lineWidth = getTextWidth(line, font) * scale
                val alignedX = startX + round((maxWidthScaled - lineWidth) * horAlign.offset)
                val alignedY = startY + round(lineIndex * font.size * scale * verAlign.offset)
                var cursorX = alignedX

                for (char in line) {
                    val charInfo = font.getCharacter(char)
                    render(
                        font.textureId,
                        cursorX + (charInfo.xOffset * scale),
                        alignedY - (charInfo.yOffset * scale),
                        charInfo.inAtlasWidth * scale,
                        charInfo.inAtlasHeight * scale,
                        charInfo.model.uvX, charInfo.model.uvY,
                        charInfo.model.uvMX, charInfo.model.uvMY,
                        zIndex,
                        color,
                        outlineThickness, angle,
                        ignoreZoom, ignoreCamera,
                        RenderType.TEXT.value,
                        effect.value
                    )
                    cursorX += charInfo.advanceWidth * scale
                }

                lineIndex++
                lineStartIdx = i + 1
            }
        }
    }

    fun renderRectangle(
        x: Float, y: Float,
        width: Float, height: Float,
        zIndex: Int = 0,
        color: Int = -1,
        angle: Float = 0f,
        thickness: Float = 1f,
        ignoreZoom: Boolean = true,
        ignoreCamera: Boolean = false
    ) {
        render(
            Textures.BLANK.id,
            x, y, width, height,
            Textures.BLANK.model.uvX,
            Textures.BLANK.model.uvY,
            Textures.BLANK.model.uvMX,
            Textures.BLANK.model.uvMY,
            zIndex, color,
            thickness, angle,
            ignoreZoom, ignoreCamera,
            RenderType.HOLL_RECT.value
        )
    }

    fun renderFilledRectangle(
        x: Float, y: Float,
        width: Float, height: Float,
        zIndex: Int = 0,
        color: Int = -1,
        angle: Float = 0f,
        ignoreZoom: Boolean = true,
        ignoreCamera: Boolean = false
    ) {
        render(
            Textures.BLANK.id,
            x, y, width, height,
            Textures.BLANK.model.uvX,
            Textures.BLANK.model.uvY,
            Textures.BLANK.model.uvMX,
            Textures.BLANK.model.uvMY,
            zIndex, color,
            1f, angle,
            ignoreZoom, ignoreCamera,
            RenderType.FILL_RECT.value
        )
    }

    fun renderLine(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        zIndex: Int = 0,
        thickness: Float = 1f,
        color: Int = -1,
        ignoreZoom: Boolean = true,
        ignoreCamera: Boolean = false
    ) {
        render(
            Textures.BLANK.id,
            x1, y1, x2, y2,
            Textures.BLANK.model.uvX,
            Textures.BLANK.model.uvY,
            Textures.BLANK.model.uvMX,
            Textures.BLANK.model.uvMY,
            zIndex,
            color,
            thickness, 0f,
            ignoreZoom, ignoreCamera,
            RenderType.LINE.value
        )
    }

    private fun render(
        texture: Int,
        x: Float, y: Float, width: Float, height: Float,
        uvX: Float, uvY: Float, uvMX: Float, uvMY: Float,
        zIndex: Int,
        color: Int = -1,
        thickness: Float, rotation: Float,
        ignoreZoom: Boolean,
        ignoreCamera: Boolean,
        renderType: Float,
        textEffect: Float = 0f
    ) {
        val cmd = commandPool[commandCount]
        cmd.tId = texture
        cmd.x = x
        cmd.y = y
        cmd.w = width
        cmd.h = height
        cmd.uvX = uvX
        cmd.uvY = uvY
        cmd.uvMX = uvMX
        cmd.uvMY = uvMY
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

        allCommands[commandCount++] = cmd
    }

    fun flush() {
        if (commandCount == 0) return

        drawCalls = 0
        totalCommandCount = commandCount

        val startSort = System.nanoTime()
        Arrays.sort(allCommands, 0, commandCount, zIndexComparator)
        sortTime = System.nanoTime() - startSort

        val currentTime = (System.currentTimeMillis() - startTimeMillis) / 1000.0f
        shader.bind()
        shader.setTime(currentTime)

        val startRender = System.nanoTime()
        renderAllBatches()
        renderTime = System.nanoTime() - startRender

        commandCount = 0
    }

    fun getStats(): String {
        return """
            |Commands: $totalCommandCount
            |Sort: ${sortTime / 1_000_000.0}ms
            |Render: ${renderTime / 1_000_000.0}ms
            |DrawCalls: $drawCalls
        """.trimMargin()
    }

    private fun renderAllBatches() {
        if (commandCount == 0) return

        var currentTexture = allCommands[0]!!.tId
        var batchStart = 0

        for (i in 1 until commandCount) {
            val cmd = allCommands[i]!!
            if (cmd.tId != currentTexture) {
                renderBatch(currentTexture, batchStart, i)
                currentTexture = cmd.tId
                batchStart = i
            }
        }

        renderBatch(currentTexture, batchStart, commandCount)
    }

    private fun renderBatch(textureId: Int, start: Int, end: Int) {
        shader.bind()
        glBindVertexArray(vao)
        TextureManager.bindTex(textureId)

        bufferData.clear()

        for (i in start until end) {
            val cmd = allCommands[i]!!
            tempBufferStore[0] = cmd.x
            tempBufferStore[1] = cmd.y
            tempBufferStore[2] = cmd.w
            tempBufferStore[3] = cmd.h
            tempBufferStore[4] = cmd.uvX
            tempBufferStore[5] = cmd.uvY
            tempBufferStore[6] = cmd.uvMX
            tempBufferStore[7] = cmd.uvMY
            tempBufferStore[8] = Float.fromBits(cmd.packedColor)  // Передаем Int как Float битовым образом
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

        drawCalls++
    }

    fun register() {
        shader = BatchShader()
        vao = glGenVertexArrays()
        vbo = glGenBuffers()

        shader.register(window)
        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        glBufferData(GL_ARRAY_BUFFER, bufferSizeBytes.toLong(), GL_STREAM_DRAW)

        offsetBytes = 0

        addAttrib(0, 4)  // rectData
        addAttrib(1, 4)  // uvData
        addIntAttrib(2, 1)  // packedColor
        addAttrib(3, 2)  // transformData
        addAttrib(4, 1)  // ignoreZoom
        addAttrib(5, 1)  // ignoreCamera
        addAttrib(6, 1)  // renderType
        addAttrib(7, 4)  // scissorRect
        addAttrib(8, 1)  // textEffect

        glBindBuffer(GL_ARRAY_BUFFER, 0)

        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        val c = window.bgColor
        glClearColor(c.r, c.g, c.b, c.a)
    }

    private fun addAttrib(index: Int, componentCount: Int) {
        glVertexAttribPointer(index, componentCount, GL_FLOAT, false, floatsPerSpriteBytes, offsetBytes)
        glEnableVertexAttribArray(index)
        glVertexAttribDivisor(index, 1)

        offsetBytes += componentCount * Float.SIZE_BYTES
    }

    private fun addIntAttrib(index: Int, componentCount: Int) {
        glVertexAttribIPointer(index, componentCount, GL_UNSIGNED_INT, floatsPerSpriteBytes, offsetBytes)
        glEnableVertexAttribArray(index)
        glVertexAttribDivisor(index, 1)

        offsetBytes += componentCount * Int.SIZE_BYTES
    }

    fun updateProjection() {
        shader.bind()
        shader.setProjection(Matrix4f().setOrtho2D(0f, window.width.toFloat(), window.height.toFloat(), 0f))
        shader.setCameraCenter(window.centerX, window.centerY)
    }

    fun setCameraScale(scale: Float) {
        cameraScale = scale
        shader.bind()
        shader.setCameraScale(cameraScale, cameraScale)
    }

    fun setCameraOffset(offset: Point<Float>) {
        cameraX = -offset.x
        cameraY = -offset.y
        shader.bind()
        shader.setCameraOffset(offset.x, offset.y)
    }

    private fun getTextWidth(text: String, font: Fonts) = text.sumOf { font.getCharacter(it).advanceWidth }
}