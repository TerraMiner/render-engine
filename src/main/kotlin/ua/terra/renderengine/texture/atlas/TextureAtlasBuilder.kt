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
import org.lwjgl.system.MemoryUtil
import ua.terra.renderengine.texture.ImageData
import ua.terra.renderengine.texture.TextureManager
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.sqrt

object TextureAtlasBuilder {

    private data class PackedRect(
        val path: String,
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )

    fun build(textures: MutableList<String>, padding: Int): TextureAtlas {
        val loaded = mutableListOf<Pair<String, ImageData>>()

        loaded.add("" to blankTexture())

        textures.forEach {
            loaded.add(it to TextureManager.loadImage(it))
        }

        loaded.sortByDescending { it.second.width * it.second.height }

        val totalArea = loaded.sumOf { (it.second.width + padding) * (it.second.height + padding) }
        val minSize = ceil(sqrt(totalArea.toDouble() * 1.3)).toInt()

        var atlasSize = nextPowerOfTwo(minSize)

        val maxTextureDimension = loaded.maxOf { maxOf(it.second.width, it.second.height) + padding }
        if (atlasSize < maxTextureDimension) {
            atlasSize = nextPowerOfTwo(maxTextureDimension)
        }

        var packed: List<PackedRect>? = null
        var attempts = 0
        val maxAtlasSize = 8192

        while (packed == null && atlasSize <= maxAtlasSize) {
            packed = tryPackTextures(loaded, atlasSize, padding)
            if (packed == null) {
                println("Atlas size $atlasSize too small, trying ${atlasSize * 2}...")
                atlasSize *= 2
                attempts++
            }
        }

        if (packed == null) {
            error("Cannot pack textures even with atlas size $atlasSize. Too many or too large textures!")
        }

        println("Textures packed successfully into ${atlasSize}x${atlasSize} atlas (attempts: ${attempts + 1})")

        val atlasBuffer = MemoryUtil.memAlloc(atlasSize * atlasSize * 4)

        for (rect in packed) {
            val texture = loaded.first { it.first == rect.path }
            copyTextureToAtlas(texture.second, rect, atlasBuffer, atlasSize)
        }

        atlasBuffer.flip()

        val textureId = createGLTexture(atlasBuffer, atlasSize)

        loaded.forEach { pair ->
            pair.second.buffer.rewind()
            pair.second.free()
        }

        MemoryUtil.memFree(atlasBuffer)

        val regions = packed.associate { rect ->
            val uvX = rect.x.toFloat() / atlasSize
            val uvY = rect.y.toFloat() / atlasSize
            val uvMX = (rect.x + rect.width).toFloat() / atlasSize
            val uvMY = (rect.y + rect.height).toFloat() / atlasSize

            rect.path to AtlasRegion(
                uvX, uvY, uvMX, uvMY,
                rect.x, rect.y, rect.width, rect.height
            )
        }

        return TextureAtlas(textureId, atlasSize, atlasSize, regions)
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

    private fun tryPackTextures(
        textures: List<Pair<String, ImageData>>,
        atlasSize: Int,
        padding: Int
    ): List<PackedRect>? {
        val packed = mutableListOf<PackedRect>()
        var shelfY = 0
        var shelfHeight = 0
        var shelfX = 0

        for (texture in textures) {
            val w = texture.second.width + padding
            val h = texture.second.height + padding

            if (shelfX + w > atlasSize) {
                shelfY += shelfHeight
                shelfX = 0
                shelfHeight = 0
            }

            if (shelfY + h > atlasSize) {
                return null
            }

            packed.add(
                PackedRect(
                    texture.first,
                    shelfX,
                    shelfY,
                    texture.second.width,
                    texture.second.height
                )
            )

            shelfX += w
            shelfHeight = maxOf(shelfHeight, h)
        }

        return packed
    }

    private fun copyTextureToAtlas(
        texture: ImageData,
        rect: PackedRect,
        atlasBuffer: ByteBuffer,
        atlasSize: Int
    ) {
        val srcData = texture.buffer
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

    private fun blankTexture(): ImageData {
        val buffer = MemoryUtil.memAlloc(4).apply {
            put(255.toByte()).put(255.toByte()).put(255.toByte()).put(255.toByte())
            flip()
        }
        return ImageData(1, 1, 4, buffer,false)
    }
}