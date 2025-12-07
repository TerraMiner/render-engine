package ua.terra.renderengine.texture.atlas

import ua.terra.renderengine.texture.manager.TextureManager
import ua.terra.renderengine.texture.model.Model
import ua.terra.renderengine.texture.source.TextureHolder

/**
 * Represents a texture within a texture atlas.
 * Lazily loads region and model data from the atlas.
 * @property atlas The parent texture atlas
 * @property path The path/name of the texture in the atlas
 */
class AtlasTexture(
    private val atlas: TexturesAtlas,
    private val path: String
) : TextureHolder {

    private var _region: AtlasRegion? = null
    private var _model: Model? = null

    private val region: AtlasRegion
        get() = _region ?: atlas.getRegion(path).also { _region = it }

    override val textureId: Int
        get() = atlas.textureId

    override val width: Int
        get() = region.pixelWidth

    override val height: Int
        get() = region.pixelHeight

    override val model: Model
        get() = _model ?: atlas.getModel(path).also { _model = it }

    override fun bind() {
        TextureManager.bindTex(textureId)
    }

    override fun unbind() {
        TextureManager.unbindTex()
    }
}