package ua.terra.renderengine.texture.source

import ua.terra.renderengine.texture.model.Model
import ua.terra.renderengine.texture.manager.RawTexture
import ua.terra.renderengine.texture.manager.TextureManager

class SingleTexture(
    private val path: String
) : TextureHolder {

    private var _texture: RawTexture? = null
    private var _model: Model? = null

    private val texture: RawTexture
        get() = _texture ?: TextureManager.get(path).also { _texture = it }

    override val textureId: Int
        get() = texture.id

    override val width: Int
        get() = texture.width

    override val height: Int
        get() = texture.height

    override val model: Model
        get() = _model ?: Model.DEFAULT.also { _model = it }

    override fun bind() {
        texture.bind()
    }

    override fun unbind() {
        texture.unbind()
    }
}