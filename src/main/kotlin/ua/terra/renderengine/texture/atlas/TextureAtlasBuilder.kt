package ua.terra.renderengine.texture.atlas

import org.lwjgl.opengl.GL11.GL_NEAREST
import org.lwjgl.opengl.GL11.GL_RGBA
import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER
import org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER
import org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S
import org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T
import org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL11.glGenTextures
import org.lwjgl.opengl.GL11.glTexImage2D
import org.lwjgl.opengl.GL11.glTexParameteri
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.stb.STBImageWrite.stbi_write_png
import org.lwjgl.system.MemoryUtil
import ua.terra.renderengine.texture.model.ImageData
import ua.terra.renderengine.texture.manager.TextureManager
import ua.terra.renderengine.texture.registry.TextureRegistry.Companion.BLANK_TEXTURE_PATH
import ua.terra.renderengine.util.MAX_ATLAS_SIZE
import ua.terra.renderengine.util.PathUtil
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt

object TextureAtlasBuilder {

    private data class PackedRect(
        val path: String,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )

    private data class AtlasMetadata(
        val paths: Set<String>,
        val atlasSize: Int,
        val padding: Int
    )

    fun build(textures: Set<String>, cachePath: String, padding: Int): TexturesAtlas {
        val cacheDir = File(cachePath)
        cacheDir.mkdirs()

        val metaFile = File(cacheDir, "texture_atlas_${textures.size}.meta")
        val atlasFile = File(cacheDir, "texture_atlas_${textures.size}.png")

        if (metaFile.exists() && atlasFile.exists()) {
            val cachedAtlas = tryLoadFromCache(textures, padding, metaFile, atlasFile)
            if (cachedAtlas != null) {
                println("Texture atlas loaded from cache (${textures.size} textures)")
                return cachedAtlas
            }
        }

        println("Building new texture atlas (${textures.size} textures)...")
        return buildNewAtlas(textures, padding, metaFile, atlasFile)
    }

    private fun tryLoadFromCache(
        textures: Set<String>,
        padding: Int,
        metaFile: File,
        atlasFile: File
    ): TexturesAtlas? {
        return try {
            val lines = metaFile.readLines()
            if (lines.size < 3) return null

            val cachedSize = lines[0].substringAfter("atlasSize=").toInt()
            val cachedPadding = lines[1].substringAfter("padding=").toInt()
            val cachedPaths = lines[2].substringAfter("paths=").split(";").toSet()

            if (cachedPadding != padding) return null
            if (cachedPaths != textures + BLANK_TEXTURE_PATH) return null

            val regions = mutableMapOf<String, AtlasRegion>()

            for (i in 3 until lines.size) {
                val line = lines[i]
                if (line.startsWith("region=")) {
                    val parts = line.substringAfter("=").split(",")
                    if (parts.size == 9) {
                        val path = parts[0]
                        regions[path] = AtlasRegion(
                            uvX = parts[1].toFloat(),
                            uvY = parts[2].toFloat(),
                            uvMX = parts[3].toFloat(),
                            uvMY = parts[4].toFloat(),
                            pixelX = parts[5].toInt(),
                            pixelY = parts[6].toInt(),
                            pixelWidth = parts[7].toInt(),
                            pixelHeight = parts[8].toInt()
                        )
                    }
                }
            }

            if (regions.isEmpty()) return null

            val imageData = TextureManager.loadImage(atlasFile.absolutePath)
            val textureId = createGLTexture(imageData)
            imageData.free()

            TexturesAtlas(textureId, cachedSize, cachedSize, regions)
        } catch (e: Exception) {
            println("Failed to load atlas from cache: ${e.message}")
            null
        }
    }

    private fun buildNewAtlas(
        textures: Set<String>,
        padding: Int,
        metaFile: File,
        atlasFile: File
    ): TexturesAtlas {
        val loaded = prepareTextures(textures)
        val atlasSize = calculateAtlasSize(loaded, padding)
        val packed = packTextures(loaded, atlasSize, padding)
        val atlasBuffer = createAtlasBuffer(loaded, packed, atlasSize)

        saveAtlasImage(atlasBuffer, atlasSize, atlasFile)

        val textureId = createGLTexture(atlasBuffer, atlasSize)
        val regions = createRegions(packed, atlasSize)

        saveMetaFile(metaFile, textures, atlasSize, padding, regions)

        loaded.forEach {
            it.second.buffer.rewind()
            it.second.free()
        }

        MemoryUtil.memFree(atlasBuffer)

        return TexturesAtlas(textureId, atlasSize, atlasSize, regions)
    }

    private fun prepareTextures(textures: Set<String>): MutableList<Pair<String, ImageData>> {
        val loaded = mutableListOf<Pair<String, ImageData>>()
        loaded.add(BLANK_TEXTURE_PATH to blankTexture())

        textures.forEach {
            try {
                val normalizedPath = PathUtil.normalize(it)
                loaded.add(it to TextureManager.loadImage(normalizedPath))
            } catch (e: Exception) {
                System.err.println("Failed to load texture $it: ${e.message}")
                throw e
            }
        }

        loaded.sortByDescending { it.second.width * it.second.height }
        return loaded
    }

    private fun calculateAtlasSize(
        loaded: List<Pair<String, ImageData>>,
        padding: Int
    ): Int {
        val totalArea = loaded.sumOf { (it.second.width + padding) * (it.second.height + padding) }
        val minSize = ceil(sqrt(totalArea.toDouble() * 1.3)).toInt()
        var atlasSize = nextPowerOfTwo(minSize)

        val maxTextureDimension = loaded.maxOf { maxOf(it.second.width, it.second.height) + padding }
        if (atlasSize < maxTextureDimension) {
            atlasSize = nextPowerOfTwo(maxTextureDimension)
        }

        return atlasSize
    }

    private fun packTextures(
        loaded: List<Pair<String, ImageData>>,
        initialAtlasSize: Int,
        padding: Int
    ): List<PackedRect> {
        var atlasSize = initialAtlasSize
        var packed: List<PackedRect>? = null
        val maxAtlasSize = MAX_ATLAS_SIZE

        while (packed == null && atlasSize <= maxAtlasSize) {
            packed = tryPackTexturesMaxRects(loaded, atlasSize, padding)
            if (packed == null) {
                println("Atlas size $atlasSize too small, trying ${atlasSize * 2}...")
                atlasSize *= 2
            }
        }

        if (packed == null) {
            error("Cannot pack ${loaded.size} textures into ${maxAtlasSize}x${maxAtlasSize} atlas! " +
                    "Consider reducing texture count or using multiple atlases.")
        }

        println("Textures packed into ${atlasSize}x${atlasSize} atlas")
        return packed
    }

    private fun tryPackTexturesMaxRects(
        textures: List<Pair<String, ImageData>>,
        atlasSize: Int,
        padding: Int
    ): List<PackedRect>? {
        val packed = mutableListOf<PackedRect>()
        val freeRects = mutableListOf(Rect(0, 0, atlasSize, atlasSize))

        for ((path, image) in textures) {
            val w = image.width + padding
            val h = image.height + padding

            val bestRect = findBestRect(freeRects, w, h) ?: return null

            packed.add(PackedRect(path, bestRect.x, bestRect.y, image.width, image.height))

            splitRect(freeRects, bestRect, w, h)
            pruneFreeRects(freeRects)
        }

        return packed
    }

    private data class Rect(val x: Int, val y: Int, val width: Int, val height: Int)

    private fun findBestRect(freeRects: List<Rect>, w: Int, h: Int): Rect? {
        var bestRect: Rect? = null
        var bestShortSideFit = Int.MAX_VALUE

        for (rect in freeRects) {
            if (rect.width >= w && rect.height >= h) {
                val leftoverX = rect.width - w
                val leftoverY = rect.height - h
                val shortSideFit = min(leftoverX, leftoverY)

                if (shortSideFit < bestShortSideFit) {
                    bestRect = rect
                    bestShortSideFit = shortSideFit
                }
            }
        }

        return bestRect
    }

    private fun splitRect(freeRects: MutableList<Rect>, usedRect: Rect, w: Int, h: Int) {
        freeRects.remove(usedRect)

        if (usedRect.width > w) {
            freeRects.add(Rect(usedRect.x + w, usedRect.y, usedRect.width - w, h))
        }

        if (usedRect.height > h) {
            freeRects.add(Rect(usedRect.x, usedRect.y + h, usedRect.width, usedRect.height - h))
        }
    }

    private fun pruneFreeRects(freeRects: MutableList<Rect>) {
        val toRemove = mutableListOf<Rect>()

        for (i in freeRects.indices) {
            for (j in freeRects.indices) {
                if (i != j && isContainedIn(freeRects[i], freeRects[j])) {
                    toRemove.add(freeRects[i])
                    break
                }
            }
        }

        freeRects.removeAll(toRemove)
    }

    private fun isContainedIn(a: Rect, b: Rect): Boolean {
        return a.x >= b.x && a.y >= b.y &&
                a.x + a.width <= b.x + b.width &&
                a.y + a.height <= b.y + b.height
    }

    private fun createAtlasBuffer(
        loaded: List<Pair<String, ImageData>>,
        packed: List<PackedRect>,
        atlasSize: Int
    ): ByteBuffer {
        val atlasBuffer = MemoryUtil.memAlloc(atlasSize * atlasSize * 4)

        for (rect in packed) {
            val texture = loaded.first { it.first == rect.path }
            copyTextureToAtlas(texture.second, rect, atlasBuffer, atlasSize)
        }

        atlasBuffer.position(0)
        atlasBuffer.limit(atlasSize * atlasSize * 4)
        return atlasBuffer
    }

    private fun saveAtlasImage(buffer: ByteBuffer, size: Int, file: File) {
        val tempFile = File.createTempFile("texture_atlas", ".png")
        tempFile.deleteOnExit()

        if (!stbi_write_png(tempFile.absolutePath, size, size, 4, buffer, size * 4)) {
            throw RuntimeException("Failed to write atlas PNG")
        }

        tempFile.copyTo(file, overwrite = true)
    }

    private fun createRegions(packed: List<PackedRect>, atlasSize: Int): Map<String, AtlasRegion> {
        return packed.associate { rect ->
            val uvX = rect.x.toFloat() / atlasSize
            val uvY = rect.y.toFloat() / atlasSize
            val uvMX = (rect.x + rect.width).toFloat() / atlasSize
            val uvMY = (rect.y + rect.height).toFloat() / atlasSize

            rect.path to AtlasRegion(
                uvX, uvY, uvMX, uvMY,
                rect.x, rect.y, rect.width, rect.height
            )
        }
    }

    private fun saveMetaFile(
        metaFile: File,
        textures: Set<String>,
        atlasSize: Int,
        padding: Int,
        regions: Map<String, AtlasRegion>
    ) {
        metaFile.bufferedWriter().use { writer ->
            writer.write("atlasSize=$atlasSize\n")
            writer.write("padding=$padding\n")
            writer.write("paths=${(textures + BLANK_TEXTURE_PATH).joinToString(";")}\n")

            regions.forEach { (path, region) ->
                writer.write("region=$path,${region.uvX},${region.uvY},${region.uvMX},${region.uvMY}," +
                        "${region.pixelX},${region.pixelY},${region.pixelWidth},${region.pixelHeight}\n")
            }
        }
    }

    private fun nextPowerOfTwo(value: Int): Int {
        var v = value - 1
        v = v or (v shr 1)
        v = v or (v shr 2)
        v = v or (v shr 4)
        v = v or (v shr 8)
        v = v or (v shr 16)
        return v + 1
    }

    private fun copyTextureToAtlas(
        texture: ImageData,
        rect: PackedRect,
        atlasBuffer: ByteBuffer,
        atlasSize: Int
    ) {
        val srcData = texture.buffer
        srcData.rewind()
        val bytesPerPixel = texture.channels

        val expectedBytes = texture.width * texture.height * bytesPerPixel
        if (srcData.remaining() < expectedBytes) {
            error("Texture buffer for ${rect.path} too small. Expected $expectedBytes bytes, but only ${srcData.remaining()} remaining")
        }

        for (y in 0 until texture.height) {
            val dstPos = ((rect.y + y) * atlasSize + rect.x) * 4
            atlasBuffer.position(dstPos)

            for (x in 0 until texture.width) {
                val r = srcData.get()
                val g = if (bytesPerPixel >= 3) srcData.get() else r
                val b = if (bytesPerPixel >= 3) srcData.get() else r
                val a = if (bytesPerPixel == 4) srcData.get() else 255.toByte()

                atlasBuffer.put(r)
                atlasBuffer.put(g)
                atlasBuffer.put(b)
                atlasBuffer.put(a)
            }
        }
    }

    private fun createGLTexture(data: ByteBuffer, size: Int): Int {
        val textureId = glGenTextures()
        TextureManager.bindTex(textureId)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

        glTexImage2D(
            GL_TEXTURE_2D, 0, GL_RGBA,
            size, size, 0,
            GL_RGBA, GL_UNSIGNED_BYTE, data
        )

        TextureManager.unbindTex()
        return textureId
    }

    private fun createGLTexture(imageData: ImageData): Int {
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
        return textureId
    }

    private fun blankTexture(): ImageData {
        val buffer = MemoryUtil.memAlloc(4).apply {
            put(255.toByte()).put(255.toByte()).put(255.toByte()).put(255.toByte())
            flip()
        }
        return ImageData(1, 1, 4, buffer, false)
    }
}