package ua.terra.renderengine.text.fonts


enum class Fonts(private val path: String, private val atlasSize: Int) {
    ANDYBOLD("fonts/AndyBold.otf", 64);

    val font: Font by lazy { Font(path, atlasSize) }
    val size: Int get() = font.size
    val textureId: Int get() = font.imageFont.id

    fun getCharacter(codepoint: Char) = font.getCharacter(codepoint)

    companion object {
        fun loadAll() = entries.forEach { it.font }
    }
}