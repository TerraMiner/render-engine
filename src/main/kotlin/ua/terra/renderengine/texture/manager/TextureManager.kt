package ua.terra.renderengine.texture.manager

import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.glTexImage2D
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL13
import org.lwjgl.stb.STBImage
import ua.terra.renderengine.texture.model.ImageData

/**
 * Manager for OpenGL texture operations.
 * Handles texture loading, binding, caching, and lifecycle management.
 */
object TextureManager {

    var boundTexture: Int = -1
    var boundUnit: Int = -1
    var textureAmount: Int = 0
    val textureCache = HashMap<String, RawTexture>()

    fun bindUnit(unit: Int = GL13.GL_TEXTURE0) {
        if (boundUnit == unit) return
        GL13.glActiveTexture(unit)
        boundUnit = unit
    }

    fun bindTex(id: Int) {
        if (boundTexture == id) return
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, id)
        boundTexture = id
    }

    fun unbindTex() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
        boundTexture = 0
    }

    fun get(path: String) = textureCache.getOrPut(path) { loadTexture(path) }

    fun clearCache() {
        textureCache.values.forEach { texture ->
            GL11.glDeleteTextures(texture.id)
        }
        textureCache.clear()
        textureAmount = 0
    }

    fun remove(path: String) {
        textureCache.remove(path)?.let { texture ->
            GL11.glDeleteTextures(texture.id)
            textureAmount--
        }
    }

    fun loadImage(path: String, channels: Int? = null): ImageData {
        val widthBuffer = BufferUtils.createIntBuffer(1)
        val heightBuffer = BufferUtils.createIntBuffer(1)
        val channelsBuffer = BufferUtils.createIntBuffer(1)

        val data = try {
            STBImage.stbi_load(path, widthBuffer, heightBuffer, channelsBuffer, channels ?: 0)
                ?: throw IllegalStateException("STB Image loading failed for: $path. Reason: ${STBImage.stbi_failure_reason()}")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load image: $path", e)
        }

        return ImageData(
            widthBuffer[0],
            heightBuffer[0],
            channels ?: channelsBuffer[0],
            data
        )
    }

    fun loadToGLFWImage(fileName: String, image: GLFWImage, channels: Int? = null): ImageData {
        val imageData = loadImage(fileName, channels)
        try {
            image[imageData.width, imageData.height] = imageData.buffer
        } catch (e: Exception) {
            imageData.free()
            throw IllegalStateException("Failed to set GLFW image from: $fileName", e)
        }
        return imageData
    }

    fun loadToGLFWImageBuffer(fileName: String, index: Int, image: GLFWImage.Buffer, channels: Int? = null): ImageData {
        val imageData = loadImage(fileName, channels)
        try {
            image.position(index)
            image.width(imageData.width)
            image.height(imageData.height)
            image.pixels(imageData.buffer)
        } catch (e: Exception) {
            imageData.free()
            throw IllegalStateException("Failed to set GLFW image buffer from: $fileName at index $index", e)
        }
        return imageData
    }

    fun getFormat(channels: Int): Int = when (channels) {
        4 -> GL11.GL_RGBA
        3 -> GL11.GL_RGB
        else -> throw IllegalArgumentException("Unsupported channel count: $channels")
    }

    fun createTexture(image: ImageData): Int {
        val textureId = GL11.glGenTextures()
        try {
            bindTex(textureId)

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

            val format = getFormat(image.channels)

            glTexImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                format,
                image.width,
                image.height,
                0,
                format,
                GL11.GL_UNSIGNED_BYTE,
                image.buffer
            )

            unbindTex()
        } catch (e: Exception) {
            GL11.glDeleteTextures(textureId)
            throw IllegalStateException("Failed to create GL texture", e)
        }

        return textureId
    }

    fun loadTexture(fileName: String): RawTexture {
        val imageData = loadImage(fileName)
        return try {
            val textureId = createTexture(imageData)
            RawTexture(textureId, imageData.width, imageData.height)
        } finally {
            imageData.free()
        }
    }
}