package ua.terra.renderengine.renderer

import ua.terra.renderengine.RenderEngine
import ua.terra.renderengine.RenderType
import ua.terra.renderengine.texture.manager.RawTexture
import ua.terra.renderengine.texture.model.Model
import ua.terra.renderengine.texture.source.TextureHolder

/**
 * Renderer for drawing textured quads.
 * Provides various overloads for rendering textures with different parameters.
 */
class TextureRenderer(private val engine: RenderEngine) {

    fun render(
        texture: RawTexture,
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
            texture.id,
            model,
            x, y, width, height,
            zIndex, angle, color,
            thickness, ignoreZoom, ignoreCamera
        )
    }

    fun render(
        texture: TextureHolder,
        x: Float, y: Float,
        width: Float, height: Float,
        zIndex: Int = 0,
        angle: Float = 0f,
        color: Int = -1,
        thickness: Float = 1f,
        ignoreZoom: Boolean = true,
        ignoreCamera: Boolean = false
    ) {
        texture.render(this,x,y,width,height,zIndex,angle,color,thickness,ignoreZoom,ignoreCamera)
    }

    fun render(
        textureId: Int,
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
        engine.submitCommand(
            textureId,
            x, y, width, height,
            model.minU, model.minV, model.maxU, model.maxV,
            zIndex, color,
            thickness, angle,
            ignoreZoom, ignoreCamera,
            RenderType.TEXTURE.value
        )
    }
}