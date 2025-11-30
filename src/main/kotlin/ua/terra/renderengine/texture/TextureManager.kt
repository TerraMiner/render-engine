package ua.terra.renderengine.texture

import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.opengl.GL11.GL_NEAREST
import org.lwjgl.opengl.GL11.GL_RGB
import org.lwjgl.opengl.GL11.GL_RGBA
import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER
import org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER
import org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S
import org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T
import org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL11.glBindTexture
import org.lwjgl.opengl.GL11.glGenTextures
import org.lwjgl.opengl.GL11.glTexImage2D
import org.lwjgl.opengl.GL11.glTexParameteri
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL13.glActiveTexture
import org.lwjgl.stb.STBImage
import ua.terra.renderengine.util.FileUtil

object TextureManager {

    var boundTexture: Int = -1
    var boundUnit: Int = -1
    var textureAmount: Int = 0
    val textureCache = HashMap<String, Texture>()

    fun bindUnit(unit: Int = GL_TEXTURE0) {
        if (boundUnit == unit) return
        glActiveTexture(unit)
        boundUnit = unit
    }

    fun bindTex(id: Int) {
        if (boundTexture == id) return
        glBindTexture(GL_TEXTURE_2D, id)
        boundTexture = id
    }

    fun unbindTex() {
        glBindTexture(GL_TEXTURE_2D, 0)
        boundTexture = 0
    }

    fun get(path: String) = textureCache.getOrPut(path) { loadTexture(path) }

    fun loadImage(fileName: String, channels: Int? = null): ImageData {
        val widthBuffer = BufferUtils.createIntBuffer(1)
        val heightBuffer = BufferUtils.createIntBuffer(1)
        val channelsBuffer = BufferUtils.createIntBuffer(1)

        val data = STBImage.stbi_load(FileUtil.getPath(fileName), widthBuffer, heightBuffer, channelsBuffer, channels ?: 0)
            ?: error("Failed to load image: $fileName")

        return ImageData(
            widthBuffer.get(0),
            heightBuffer.get(0),
            channels ?: channelsBuffer.get(0),
            data
        )
    }

    fun loadToGLFWImage(fileName: String, image: GLFWImage, channels: Int? = null): ImageData {
        val imageData = loadImage(fileName, channels)
        image.set(imageData.width, imageData.height, imageData.buffer)
        return imageData
    }

    fun loadToGLFWImageBuffer(fileName: String, index: Int, image: GLFWImage.Buffer, channels: Int? = null): ImageData {
        val imageData = loadImage(fileName, channels)
        image.position(index)
        image.width(imageData.width)
        image.height(imageData.height)
        image.pixels(imageData.buffer)
        return imageData
    }

    fun getFormat(channels: Int): Int = when (channels) {
        4 -> GL_RGBA
        3 -> GL_RGB
        else -> error("Unsupported channel count: $channels")
    }

    fun createTexture(image: ImageData): Int {
        val textureId = glGenTextures()
        bindTex(textureId)

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

        val format = getFormat(image.channels)

        glTexImage2D(GL_TEXTURE_2D, 0, format, image.width, image.height, 0, format, GL_UNSIGNED_BYTE, image.buffer)

        unbindTex()

        return textureId
    }

    fun loadTexture(fileName: String): Texture {
        val imageData = loadImage(fileName)
        val textureId = createTexture(imageData)
        imageData.free()
        return Texture(textureId, imageData.width,imageData.height)
    }
}
