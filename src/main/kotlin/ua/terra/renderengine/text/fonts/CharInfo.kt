package ua.terra.renderengine.text.fonts

import ua.terra.renderengine.texture.model.Model

class CharInfo(
    val inAtlasX: Int,
    val inAtlasY: Int,
    val inAtlasWidth: Int,
    val inAtlasHeight: Int,
    val xOffset: Int = 0,
    val yOffset: Int = 0,
    val advanceWidth: Int
) {
    lateinit var model: Model

    fun buildModel(atlasWidth: Int, atlasHeight: Int) {
        val uvx = inAtlasX.toFloat() / atlasWidth
        val uvy = inAtlasY.toFloat() / atlasHeight
        val uvmx = uvx + inAtlasWidth.toFloat() / atlasWidth
        val uvmy = uvy + inAtlasHeight.toFloat() / atlasHeight
        model = Model(uvx, uvy, uvmx, uvmy)
    }
}