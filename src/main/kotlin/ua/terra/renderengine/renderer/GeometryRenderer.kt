package ua.terra.renderengine.renderer

import ua.terra.renderengine.RenderEngine
import ua.terra.renderengine.RenderType
import ua.terra.renderengine.texture.model.Model
import ua.terra.renderengine.texture.registry.TextureRegistry


class GeometryRenderer(
    private val engine: RenderEngine,
    private val textureRegistry: TextureRegistry
) {

    private val blankTexture by lazy { textureRegistry.getBlank() }
    private val blankModel = Model.DEFAULT

    fun renderRectangle(
        x: Float, y: Float,
        width: Float, height: Float,
        zIndex: Int = 0,
        color: Int = -1,
        angle: Float = 0f,
        thickness: Float = 1f,
        ignoreZoom: Boolean = true,
        ignoreCamera: Boolean = false
    ) {
        engine.submitCommand(
            blankTexture.id,
            x, y, width, height,
            blankModel.uvX,
            blankModel.uvY,
            blankModel.uvMX,
            blankModel.uvMY,
            zIndex, color,
            thickness, angle,
            ignoreZoom, ignoreCamera,
            RenderType.HOLL_RECT.value
        )
    }

    fun renderFilledRectangle(
        x: Float, y: Float,
        width: Float, height: Float,
        zIndex: Int = 0,
        color: Int = -1,
        angle: Float = 0f,
        ignoreZoom: Boolean = true,
        ignoreCamera: Boolean = false
    ) {
        engine.submitCommand(
            blankTexture.id,
            x, y, width, height,
            blankModel.uvX,
            blankModel.uvY,
            blankModel.uvMX,
            blankModel.uvMY,
            zIndex, color,
            1f, angle,
            ignoreZoom, ignoreCamera,
            RenderType.FILL_RECT.value
        )
    }

    fun renderLine(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        zIndex: Int = 0,
        thickness: Float = 1f,
        color: Int = -1,
        ignoreZoom: Boolean = true,
        ignoreCamera: Boolean = false
    ) {
        engine.submitCommand(
            blankTexture.id,
            x1, y1, x2, y2,
            blankModel.uvX,
            blankModel.uvY,
            blankModel.uvMX,
            blankModel.uvMY,
            zIndex,
            color,
            thickness, 0f,
            ignoreZoom, ignoreCamera,
            RenderType.LINE.value
        )
    }
}