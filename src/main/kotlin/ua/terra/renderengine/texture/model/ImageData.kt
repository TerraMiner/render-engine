package ua.terra.renderengine.texture.model

import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

/**
 * Holds raw image data for texture creation.
 * @property width The image width in pixels
 * @property height The image height in pixels
 * @property channels The number of color channels (e.g., 3 for RGB, 4 for RGBA)
 * @property buffer The raw pixel data buffer
 * @property isSTBImage Whether this image was loaded with STBImage
 */
data class ImageData(
    val width: Int,
    val height: Int,
    val channels: Int,
    val buffer: ByteBuffer,
    val isSTBImage: Boolean = true
) {
    /** Frees the native memory used by this image */
    fun free() {
        if (isSTBImage) {
            STBImage.stbi_image_free(buffer)
        } else {
            MemoryUtil.memFree(buffer)
        }
    }
}