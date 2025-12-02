package ua.terra.renderengine.renderer

import ua.terra.renderengine.RenderEngine
import ua.terra.renderengine.RenderType
import ua.terra.renderengine.texture.atlas.TexturesAtlas
import ua.terra.renderengine.texture.manager.RawTexture
import ua.terra.renderengine.texture.model.Model
import ua.terra.renderengine.texture.source.TextureHolder

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
        render(
            texture.textureId,
            texture.model,
            x, y, width, height,
            zIndex, angle, color,
            thickness, ignoreZoom, ignoreCamera
        )
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
            model.uvX, model.uvY, model.uvMX, model.uvMY,
            zIndex, color,
            thickness, angle,
            ignoreZoom, ignoreCamera,
            RenderType.TEXTURE.value
        )
    }

    fun renderFromAtlas(
        atlas: TexturesAtlas,
        regionName: String,
        x: Float, y: Float,
        width: Float, height: Float,
        zIndex: Int = 0,
        angle: Float = 0f,
        color: Int = -1,
        ignoreZoom: Boolean = true,
        ignoreCamera: Boolean = false
    ) {
        val model = atlas.getModel(regionName)
        render(
            atlas.textureId,
            model,
            x, y, width, height,
            zIndex, angle, color,
            1f, ignoreZoom, ignoreCamera
        )
    }
}