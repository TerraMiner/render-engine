package ua.terra.renderengine.texture.source

import ua.terra.renderengine.texture.model.Model

interface TextureHolder {
    val textureId: Int
    val width: Int
    val height: Int
    val model: Model

    fun bind()
    fun unbind()
}