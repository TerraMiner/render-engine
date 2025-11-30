package ua.terra.renderengine.texture

import ua.terra.renderengine.texture.atlas.AtlasRegion
import ua.terra.renderengine.texture.atlas.TextureAtlas
import ua.terra.renderengine.texture.atlas.TextureAtlasBuilder
import ua.terra.renderengine.texture.model.Model

data class Textures(val path: String = "") {
    private var _region: AtlasRegion? = null
    val region: AtlasRegion get() = _region ?: ATLAS.getRegion(path).also { _region = it }

    private var _model: Model? = null
    val model: Model get() = _model ?: ATLAS.getModel(path).also { _model = it }
    val id: Int get() = ATLAS.textureId
    val width: Int get() = region.pixelWidth
    val height: Int get() = region.pixelHeight

    init {
        entries.add(this)
    }

    companion object {
        lateinit var ATLAS: TextureAtlas
        val entries: MutableList<Textures> = ArrayList<Textures>()

        lateinit var BLANK: Textures

        fun loadAll() {
            BLANK = Textures()

            val texturePaths = entries.mapNotNull { it.path.takeIf(String::isNotBlank) }.toMutableList()

            ATLAS = TextureAtlasBuilder.build(texturePaths, 2)

            println("Texture atlas created: ${ATLAS.width}x${ATLAS.height}, textures: ${texturePaths.size + 1}")
        }
    }
}
