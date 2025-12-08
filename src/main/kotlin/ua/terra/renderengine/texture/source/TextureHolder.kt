package ua.terra.renderengine.texture.source

import ua.terra.renderengine.renderer.TextureRenderer
import ua.terra.renderengine.texture.model.Model

/**
 * Interface for objects that hold texture data and OpenGL texture operations.
 */
interface TextureHolder {
    /** The OpenGL texture ID */
    val textureId: Int
    /** The texture width in pixels */
    val width: Int
    /** The texture height in pixels */
    val height: Int
    /** The texture model with UV coordinates */
    val model: Model

    /** Binds this texture to the current OpenGL context */
    fun bind()
    /** Unbinds this texture from the current OpenGL context */
    fun unbind()

    /** Defines the render behavior when invoked within the TextureRenderer's render method. */
    fun render(
        renderer: TextureRenderer,
        x: Float, y: Float,
        width: Float, height: Float,
        zIndex: Int = 0,
        angle: Float = 0f,
        color: Int = -1,
        thickness: Float = 1f,
        ignoreZoom: Boolean = true,
        ignoreCamera: Boolean = false
    ) {
        renderer.render(
            textureId,
            model,
            x, y, width, height,
            zIndex, angle, color,
            thickness, ignoreZoom, ignoreCamera
        )
    }
}