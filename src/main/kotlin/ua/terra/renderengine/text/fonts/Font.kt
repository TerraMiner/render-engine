package ua.terra.renderengine.text.fonts

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.stb.STBImageWrite.stbi_write_png
import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTruetype.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import ua.terra.renderengine.texture.Texture
import ua.terra.renderengine.texture.TextureManager
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

class Font(path: String, val size: Int, val debugBorders: Boolean = false) {
    var lineHeight: Int = 0
    val characterMap = Int2ObjectOpenHashMap<CharInfo>()
    val atlasSquareSize: Int
    val imageFont: Texture

    private var ascent: Int = 0
    private var descent: Int = 0
    private var lineGap: Int = 0

    init {
        val jarFile = File(Font::class.java.protectionDomain.codeSource.location.toURI())
        val cacheDir = File(jarFile.parent, "cache")
        cacheDir.mkdirs()

        val atlasFile = File(cacheDir, "${path.substringBeforeLast('.')}_atlas_$size.png")

        val fontStream = Font::class.java.getResourceAsStream("/${path}")
            ?: throw IllegalArgumentException("Font not found: /${path}")

        val fontBytes = fontStream.readBytes()
        val fontBuffer = BufferUtils.createByteBuffer(fontBytes.size)
        fontBuffer.put(fontBytes)
        fontBuffer.flip()

        val fontInfo = STBTTFontinfo.create()
        if (!stbtt_InitFont(fontInfo, fontBuffer)) {
            throw RuntimeException("Failed to initialize font")
        }

        val scale = stbtt_ScaleForPixelHeight(fontInfo, size.toFloat())

        MemoryStack.stackPush().use { stack ->
            val pAscent = stack.mallocInt(1)
            val pDescent = stack.mallocInt(1)
            val pLineGap = stack.mallocInt(1)

            stbtt_GetFontVMetrics(fontInfo, pAscent, pDescent, pLineGap)

            ascent = (pAscent.get(0) * scale).toInt()
            descent = (pDescent.get(0) * scale).toInt()
            lineGap = (pLineGap.get(0) * scale).toInt()
            lineHeight = ascent - descent + lineGap
        }

        val availableChars = (32..2000).filter { codepoint ->
            stbtt_FindGlyphIndex(fontInfo, codepoint) != 0
        }

        val CHAR_PADDING = 6
        var maxCharSize = lineHeight

        MemoryStack.stackPush().use { stack ->
            val pAdvance = stack.mallocInt(1)
            val pLeftSideBearing = stack.mallocInt(1)
            val x0 = stack.mallocInt(1)
            val y0 = stack.mallocInt(1)
            val x1 = stack.mallocInt(1)
            val y1 = stack.mallocInt(1)

            for (codepoint in availableChars) {
                stbtt_GetCodepointHMetrics(fontInfo, codepoint, pAdvance, pLeftSideBearing)
                stbtt_GetCodepointBitmapBox(fontInfo, codepoint, scale, scale, x0, y0, x1, y1)

                val width = x1.get(0) - x0.get(0)
                val height = y1.get(0) - y0.get(0)

                maxCharSize = max(maxCharSize, max(width, height))
            }
        }

        maxCharSize += CHAR_PADDING * 2

        atlasSquareSize = ceil(sqrt(availableChars.size.toDouble())).toInt() + 1
        val atlasSize = atlasSquareSize * maxCharSize

        MemoryStack.stackPush().use { stack ->
            val pAdvance = stack.mallocInt(1)
            val pLeftSideBearing = stack.mallocInt(1)

            availableChars.forEachIndexed { i, codepoint ->
                stbtt_GetCodepointHMetrics(fontInfo, codepoint, pAdvance, pLeftSideBearing)

                val charWidth = (pAdvance.get(0) * scale).toInt()

                val pos = getCoords(i, atlasSquareSize, atlasSquareSize)
                val inAtlasX = maxCharSize * pos.first + CHAR_PADDING
                val inAtlasY = maxCharSize * pos.second + CHAR_PADDING

                val charHeight = lineHeight
                val logicalX = inAtlasX + (maxCharSize - CHAR_PADDING * 2 - charWidth) / 2
                val logicalY = inAtlasY + (maxCharSize - CHAR_PADDING * 2 - charHeight) / 2

                val xOffset = inAtlasX - logicalX
                val yOffset = inAtlasY - logicalY

                val info = CharInfo(
                    inAtlasX = inAtlasX - CHAR_PADDING,
                    inAtlasY = inAtlasY - CHAR_PADDING,
                    inAtlasWidth = maxCharSize,
                    inAtlasHeight = maxCharSize,
                    xOffset = xOffset,
                    yOffset = yOffset,
                    advanceWidth = max(1, charWidth)
                )
                characterMap[codepoint] = info
            }
        }

        buildCharInfoModels(atlasSize, atlasSize)

        if (!atlasFile.exists()) {
            val tempFile = File.createTempFile("font_atlas", ".png")
            tempFile.deleteOnExit()

            var atlasBuffer: ByteBuffer?
            atlasBuffer = BufferUtils.createByteBuffer(atlasSize * atlasSize * 4)

            for (i in 0 until atlasSize * atlasSize) {
                atlasBuffer.put(i * 4 + 0, 0.toByte())
                atlasBuffer.put(i * 4 + 1, 0.toByte())
                atlasBuffer.put(i * 4 + 2, 0.toByte())
                atlasBuffer.put(i * 4 + 3, 0.toByte())
            }

            MemoryStack.stackPush().use { stack ->
                characterMap.toSortedMap().entries.forEachIndexed { index, (codepoint, info) ->
                    val pos = getCoords(index, atlasSquareSize, atlasSquareSize)
                    val cellX = maxCharSize * pos.first + CHAR_PADDING
                    val cellY = maxCharSize * pos.second + CHAR_PADDING

                    val pWidth = stack.mallocInt(1)
                    val pHeight = stack.mallocInt(1)
                    val pXoff = stack.mallocInt(1)
                    val pYoff = stack.mallocInt(1)

                    val bitmap = stbtt_GetCodepointBitmap(
                        fontInfo, scale, scale, codepoint,
                        pWidth, pHeight, pXoff, pYoff
                    )

                    if (bitmap != null) {
                        val width = pWidth.get(0)
                        val height = pHeight.get(0)
                        val xoff = pXoff.get(0)
                        val yoff = pYoff.get(0)

                        val pAdvance = stack.mallocInt(1)
                        val pLsb = stack.mallocInt(1)
                        stbtt_GetCodepointHMetrics(fontInfo, codepoint, pAdvance, pLsb)

                        val charWidth = (pAdvance.get(0) * scale).toInt()
                        val logicalX = cellX + (maxCharSize - CHAR_PADDING * 2 - charWidth) / 2
                        val logicalY = cellY + (maxCharSize - CHAR_PADDING * 2 - lineHeight) / 2

                        val drawX = logicalX + xoff
                        val drawY = logicalY + ascent + yoff

                        for (y in 0 until height) {
                            for (x in 0 until width) {
                                val atlasX = drawX + x
                                val atlasY = drawY + y

                                if (atlasX in 0..<atlasSize && atlasY in 0..< atlasSize) {
                                    val atlasIdx = (atlasY * atlasSize + atlasX) * 4
                                    val alpha = bitmap.get(y * width + x)

                                    atlasBuffer.put(atlasIdx + 0, 0xFF.toByte())
                                    atlasBuffer.put(atlasIdx + 1, 0xFF.toByte())
                                    atlasBuffer.put(atlasIdx + 2, 0xFF.toByte())
                                    atlasBuffer.put(atlasIdx + 3, alpha)
                                }
                            }
                        }

                        stbtt_FreeBitmap(bitmap)
                    }

                    if (debugBorders) {
                        val borderStartX = cellX - CHAR_PADDING
                        val borderStartY = cellY - CHAR_PADDING
                        val borderWidth = maxCharSize
                        val borderHeight = maxCharSize

                        for (x in 0 until borderWidth) {
                            val topX = borderStartX + x
                            val topY = borderStartY
                            val bottomY = borderStartY + borderHeight - 1

                            if (topX in 0..<atlasSize) {
                                if (topY in 0..<atlasSize) {
                                    val idx = (topY * atlasSize + topX) * 4
                                    atlasBuffer.put(idx + 0, 0xFF.toByte())
                                    atlasBuffer.put(idx + 1, 0x00.toByte())
                                    atlasBuffer.put(idx + 2, 0x00.toByte())
                                    atlasBuffer.put(idx + 3, 0xFF.toByte())
                                }
                                if (bottomY in 0..<atlasSize) {
                                    val idx = (bottomY * atlasSize + topX) * 4
                                    atlasBuffer.put(idx + 0, 0xFF.toByte())
                                    atlasBuffer.put(idx + 1, 0x00.toByte())
                                    atlasBuffer.put(idx + 2, 0x00.toByte())
                                    atlasBuffer.put(idx + 3, 0xFF.toByte())
                                }
                            }
                        }

                        for (y in 0 until borderHeight) {
                            val leftX = borderStartX
                            val rightX = borderStartX + borderWidth - 1
                            val borderY = borderStartY + y

                            if (borderY in 0..<atlasSize) {
                                if (leftX in 0..<atlasSize) {
                                    val idx = (borderY * atlasSize + leftX) * 4
                                    atlasBuffer.put(idx + 0, 0xFF.toByte())
                                    atlasBuffer.put(idx + 1, 0x00.toByte())
                                    atlasBuffer.put(idx + 2, 0x00.toByte())
                                    atlasBuffer.put(idx + 3, 0xFF.toByte())
                                }
                                if (rightX in 0..<atlasSize) {
                                    val idx = (borderY * atlasSize + rightX) * 4
                                    atlasBuffer.put(idx + 0, 0xFF.toByte())
                                    atlasBuffer.put(idx + 1, 0x00.toByte())
                                    atlasBuffer.put(idx + 2, 0x00.toByte())
                                    atlasBuffer.put(idx + 3, 0xFF.toByte())
                                }
                            }
                        }
                    }
                }
            }

            if (!stbi_write_png(tempFile.absolutePath, atlasSize, atlasSize, 4, atlasBuffer, atlasSize * 4)) {
                throw RuntimeException("Failed to write PNG atlas")
            }

            MemoryUtil.memFree(atlasBuffer)
            atlasBuffer = null

            tempFile.copyTo(atlasFile, overwrite = true)
        }

        imageFont = loadTextureWithLinearFilter(atlasFile.absolutePath)
    }

    private fun loadTextureWithLinearFilter(path: String): Texture {
        val imageData = TextureManager.loadImage(path)
        val textureId = glGenTextures()

        TextureManager.bindTex(textureId)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

        val format = TextureManager.getFormat(imageData.channels)
        glTexImage2D(
            GL_TEXTURE_2D, 0, format,
            imageData.width, imageData.height, 0,
            format, GL_UNSIGNED_BYTE, imageData.buffer
        )

        TextureManager.unbindTex()
        imageData.free()

        return Texture(textureId, imageData.width, imageData.height)
    }

    private fun buildCharInfoModels(atlasWidth: Int, atlasHeight: Int) {
        characterMap.values.forEach { info ->
            info.buildModel(atlasWidth, atlasHeight)
        }
    }

    private fun getCoords(index: Int, columns: Int, rows: Int): Pair<Int, Int> {
        val x = index % columns
        val y = index / columns
        return Pair(x, y)
    }

    fun getCharacter(codepoint: Char): CharInfo {
        return characterMap[codepoint.code] ?: characterMap['?'.code]!!
    }
}