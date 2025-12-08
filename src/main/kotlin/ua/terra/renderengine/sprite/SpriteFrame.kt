package ua.terra.renderengine.sprite

import ua.terra.renderengine.texture.model.Model

/**
 * Runtime sprite frame with both pixel and UV data.
 */
data class SpriteFrame(
    val width: Int,
    val height: Int,
    val model: Model
)

