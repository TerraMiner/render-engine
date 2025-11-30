package ua.terra.renderengine.texture

import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

data class ImageData(
    val width: Int,
    val height: Int,
    val channels: Int,
    val buffer: ByteBuffer,
    val isSTBImage: Boolean = true
) {
    fun free() {
        if (isSTBImage) {
            STBImage.stbi_image_free(buffer)
        } else {
            MemoryUtil.memFree(buffer)
        }
    }
}