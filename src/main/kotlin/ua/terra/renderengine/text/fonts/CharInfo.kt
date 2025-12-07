package ua.terra.renderengine.text.fonts

import ua.terra.renderengine.texture.model.Model

/**
 * Contains information about a character's position and metrics in a font atlas.
 * @property inAtlasX The x-coordinate of the character in the atlas
 * @property inAtlasY The y-coordinate of the character in the atlas
 * @property inAtlasWidth The width of the character in the atlas
 * @property inAtlasHeight The height of the character in the atlas
 * @property xOffset Horizontal offset when rendering the character
 * @property yOffset Vertical offset when rendering the character
 * @property advanceWidth The horizontal advance to the next character
 */
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
        val minU = inAtlasX.toFloat() / atlasWidth
        val minV = inAtlasY.toFloat() / atlasHeight
        val maxU = minU + inAtlasWidth.toFloat() / atlasWidth
        val maxV = minV + inAtlasHeight.toFloat() / atlasHeight
        model = Model(minU, minV, maxU, maxV)
    }
}