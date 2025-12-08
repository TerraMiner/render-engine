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
import ua.terra.renderengine.texture.manager.RawTexture
import ua.terra.renderengine.texture.manager.TextureManager
import ua.terra.renderengine.util.getCoords
import java.io.File
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

object FontLoader {
    fun load(path: String, cachePath: String, size: Int): Font {
        val cacheDir = File(cachePath)
        cacheDir.mkdirs()

        val fontFileName = File(path).nameWithoutExtension
        val atlasFile = File(cacheDir, "${fontFileName}_atlas_$size.png")
        val metaFile = File(cacheDir, "${fontFileName}_atlas_$size.meta")

        if (atlasFile.exists() && metaFile.exists()) {
            val cachedFont = tryLoadFromCache(path, size, atlasFile, metaFile)
            if (cachedFont != null) {
                println("Font $fontFileName ($size) loaded from cache")
                return cachedFont
            }
        }

        println("Generating font atlas for $fontFileName ($size)...")
        return generateFont(path, size, atlasFile, metaFile)
    }

    private fun tryLoadFromCache(
        path: String,
        size: Int,
        atlasFile: File,
        metaFile: File
    ): Font? {
        return try {
            val meta = metaFile.readText()
            val lines = meta.lines()
            if (lines.size < 2) return null

            val cachedPath = lines[0].substringAfter("path=")
            val cachedSize = lines[1].substringAfter("size=").toInt()

            if (cachedPath != path || cachedSize != size) return null

            val fontFile = File(path)
            val fontBytes = fontFile.readBytes()
            val fontBuffer = BufferUtils.createByteBuffer(fontBytes.size)
            fontBuffer.put(fontBytes).flip()

            val fontInfo = STBTTFontinfo.create()
            if (!stbtt_InitFont(fontInfo, fontBuffer)) return null
            val metrics = FontMetrics(fontInfo, size)
            val lineGap = metrics.lineGap
            val descent = metrics.descent
            val ascent = metrics.ascent

            val characterMap = Int2ObjectOpenHashMap<CharInfo>()
            var atlasSquareSize = 0

            for (i in 2 until lines.size) {
                val line = lines[i]
                if (line.startsWith("atlasSquareSize=")) {
                    atlasSquareSize = line.substringAfter("=").toInt()
                } else if (line.startsWith("char=")) {
                    val parts = line.substringAfter("=").split(",")
                    if (parts.size == 8) {
                        val codepoint = parts[0].toInt()
                        val info = CharInfo(
                            inAtlasX = parts[1].toInt(),
                            inAtlasY = parts[2].toInt(),
                            inAtlasWidth = parts[3].toInt(),
                            inAtlasHeight = parts[4].toInt(),
                            xOffset = parts[5].toInt(),
                            yOffset = parts[6].toInt(),
                            advanceWidth = parts[7].toInt()
                        )
                        characterMap[codepoint] = info
                    }
                }
            }

            val atlasSize = lines.find { it.startsWith("atlasSize=") }
                ?.substringAfter("=")?.toInt() ?: return null

            characterMap.values.forEach { it.buildModel(atlasSize, atlasSize) }

            val imageFont = loadTextureWithLinearFilter(atlasFile.absolutePath)

            Font(size, metrics.lineHeight, characterMap, atlasSquareSize, imageFont, ascent, descent,
                lineGap
            )
        } catch (e: Exception) {
            println("Failed to load font from cache: ${e.message}")
            null
        }
    }

    private fun generateFont(
        path: String,
        size: Int,
        atlasFile: File,
        metaFile: File
    ): Font {
        val fontFile = File(path)

        require(fontFile.exists()) { "Font not found: ${fontFile.absolutePath}" }

        val fontBytes = fontFile.readBytes()
        val fontBuffer = BufferUtils.createByteBuffer(fontBytes.size)
        fontBuffer.put(fontBytes).flip()

        val fontInfo = STBTTFontinfo.create()
        if (!stbtt_InitFont(fontInfo, fontBuffer)) {
            throw RuntimeException("Failed to initialize font")
        }

        val metrics = FontMetrics(fontInfo, size)

        val availableChars = (32..2000).filter { codepoint ->
            stbtt_FindGlyphIndex(fontInfo, codepoint) != 0
        }

        val charPadding = 6
        val lineHeight = metrics.lineHeight
        var maxCharSize = lineHeight

        val scale = metrics.scale
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

                val width = x1[0] - x0[0]
                val height = y1[0] - y0[0]

                maxCharSize = max(maxCharSize, max(width, height))
            }
        }

        maxCharSize += charPadding * 2

        val atlasSquareSize = ceil(sqrt(availableChars.size.toDouble())).toInt() + 1
        val atlasSize = atlasSquareSize * maxCharSize

        val characterMap = Int2ObjectOpenHashMap<CharInfo>()

        MemoryStack.stackPush().use { stack ->
            val pAdvance = stack.mallocInt(1)
            val pLeftSideBearing = stack.mallocInt(1)

            availableChars.forEachIndexed { i, codepoint ->
                stbtt_GetCodepointHMetrics(fontInfo, codepoint, pAdvance, pLeftSideBearing)

                val charWidth = (pAdvance[0] * scale).toInt()

                val pos = getCoords(i, atlasSquareSize, atlasSquareSize)
                val inAtlasX = maxCharSize * pos.x + charPadding
                val inAtlasY = maxCharSize * pos.y + charPadding

                val charHeight = lineHeight
                val logicalX = inAtlasX + (maxCharSize - charPadding * 2 - charWidth) / 2
                val logicalY = inAtlasY + (maxCharSize - charPadding * 2 - charHeight) / 2

                val xOffset = inAtlasX - logicalX
                val yOffset = inAtlasY - logicalY

                val info = CharInfo(
                    inAtlasX = inAtlasX - charPadding,
                    inAtlasY = inAtlasY - charPadding,
                    inAtlasWidth = maxCharSize,
                    inAtlasHeight = maxCharSize,
                    xOffset = xOffset,
                    yOffset = yOffset,
                    advanceWidth = max(1, charWidth)
                )
                characterMap[codepoint] = info
            }
        }

        characterMap.values.forEach { it.buildModel(atlasSize, atlasSize) }

        val ascent = metrics.ascent
        generateAtlasImage(fontInfo, characterMap, atlasSize, maxCharSize, charPadding,
            scale, ascent, lineHeight, atlasSquareSize, atlasFile)

        saveMetaFile(metaFile, path, size, characterMap, atlasSquareSize, atlasSize)

        val imageFont = loadTextureWithLinearFilter(atlasFile.absolutePath)

        val lineGap = metrics.lineGap
        val descent = metrics.descent
        return Font(size, lineHeight, characterMap, atlasSquareSize, imageFont, ascent, descent,
            lineGap
        )
    }

    private fun generateAtlasImage(
        fontInfo: STBTTFontinfo,
        characterMap: Int2ObjectOpenHashMap<CharInfo>,
        atlasSize: Int,
        maxCharSize: Int,
        charPadding: Int,
        scale: Float,
        ascent: Int,
        lineHeight: Int,
        atlasSquareSize: Int,
        atlasFile: File
    ) {
        val atlasBuffer = MemoryUtil.memAlloc(atlasSize * atlasSize * 4)

        for (i in 0 until atlasSize * atlasSize) {
            atlasBuffer.put(i * 4 + 0, 0.toByte())
            atlasBuffer.put(i * 4 + 1, 0.toByte())
            atlasBuffer.put(i * 4 + 2, 0.toByte())
            atlasBuffer.put(i * 4 + 3, 0.toByte())
        }

        atlasBuffer.rewind()

        MemoryStack.stackPush().use { stack ->
            characterMap.toSortedMap().entries.forEachIndexed { index, (codepoint, _) ->
                val pos = getCoords(index, atlasSquareSize, atlasSquareSize)
                val cellX = maxCharSize * pos.x + charPadding
                val cellY = maxCharSize * pos.y + charPadding

                val pWidth = stack.mallocInt(1)
                val pHeight = stack.mallocInt(1)
                val pXoff = stack.mallocInt(1)
                val pYoff = stack.mallocInt(1)

                val bitmap = stbtt_GetCodepointBitmap(
                    fontInfo, scale, scale, codepoint,
                    pWidth, pHeight, pXoff, pYoff
                )

                if (bitmap != null) {
                    val width = pWidth[0]
                    val height = pHeight[0]
                    val xoff = pXoff[0]
                    val yoff = pYoff[0]

                    val pAdvance = stack.mallocInt(1)
                    val pLsb = stack.mallocInt(1)
                    stbtt_GetCodepointHMetrics(fontInfo, codepoint, pAdvance, pLsb)

                    val charWidth = (pAdvance[0] * scale).toInt()
                    val logicalX = cellX + (maxCharSize - charPadding * 2 - charWidth) / 2
                    val logicalY = cellY + (maxCharSize - charPadding * 2 - lineHeight) / 2

                    val drawX = logicalX + xoff
                    val drawY = logicalY + ascent + yoff

                    for (y in 0 until height) {
                        for (x in 0 until width) {
                            val atlasX = drawX + x
                            val atlasY = drawY + y

                            if (atlasX in 0..<atlasSize && atlasY in 0..<atlasSize) {
                                val atlasIdx = (atlasY * atlasSize + atlasX) * 4
                                val alpha = bitmap[y * width + x]

                                atlasBuffer.put(atlasIdx + 0, 0xFF.toByte())
                                atlasBuffer.put(atlasIdx + 1, 0xFF.toByte())
                                atlasBuffer.put(atlasIdx + 2, 0xFF.toByte())
                                atlasBuffer.put(atlasIdx + 3, alpha)
                            }
                        }
                    }

                    stbtt_FreeBitmap(bitmap)
                }
            }
        }

        atlasBuffer.position(0)

        try {
            if (!stbi_write_png(atlasFile.absolutePath, atlasSize, atlasSize, 4, atlasBuffer, atlasSize * 4)) {
                throw RuntimeException("Failed to write PNG atlas")
            }
        } finally {
            MemoryUtil.memFree(atlasBuffer)
        }
    }

    private fun saveMetaFile(
        metaFile: File,
        path: String,
        size: Int,
        characterMap: Int2ObjectOpenHashMap<CharInfo>,
        atlasSquareSize: Int,
        atlasSize: Int
    ) {
        metaFile.bufferedWriter().use { writer ->
            writer.write("path=$path\n")
            writer.write("size=$size\n")
            writer.write("atlasSquareSize=$atlasSquareSize\n")
            writer.write("atlasSize=$atlasSize\n")
            characterMap.forEach { (codepoint, info) ->
                writer.write("char=$codepoint,${info.inAtlasX},${info.inAtlasY}," +
                        "${info.inAtlasWidth},${info.inAtlasHeight}," +
                        "${info.xOffset},${info.yOffset},${info.advanceWidth}\n")
            }
        }
    }

    private fun loadTextureWithLinearFilter(path: String): RawTexture {
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

        return RawTexture(textureId, imageData.width, imageData.height)
    }
}