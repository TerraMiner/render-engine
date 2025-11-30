package ua.terra.renderengine.texture.atlas

import ua.terra.renderengine.texture.model.Model

class TextureAtlas(
    val textureId: Int,
    val width: Int,
    val height: Int,
    private val regions: Map<String, AtlasRegion>
) {
    fun getRegion(name: String): AtlasRegion =
        regions[name] ?: regions[""]!!

    fun getModel(name: String): Model =
        getRegion(name).let { Model(it.uvX, it.uvY, it.uvMX, it.uvMY) }
}