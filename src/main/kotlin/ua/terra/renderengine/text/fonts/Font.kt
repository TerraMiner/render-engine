package ua.terra.renderengine.text.fonts

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import ua.terra.renderengine.texture.manager.RawTexture

/**
 * Represents a bitmap font with character atlas.
 * Contains character metrics, atlas texture, and rendering information.
 * @property size Font size in pixels
 * @property lineHeight Line height in pixels
 * @property characterMap Map of character code points to their info
 * @property atlasSquareSize Number of characters per row/column in atlas
 * @property imageFont OpenGL texture containing the font atlas
 */
class Font internal constructor(
    val size: Int,
    val lineHeight: Int,
    val characterMap: Int2ObjectOpenHashMap<CharInfo>,
    val atlasSquareSize: Int,
    val imageFont: RawTexture,
    private val ascent: Int,
    private val descent: Int,
    private val lineGap: Int
) : FontHolder {

    /**
     * Gets character information for a given character.
     * Returns '?' character info if character not found.
     * @param codepoint Character to get info for
     * @return CharInfo for the character
     */
    fun getCharacter(codepoint: Char): CharInfo {
        return characterMap[codepoint.code] ?: characterMap['?'.code]!!
    }

    override val font get() = this
}