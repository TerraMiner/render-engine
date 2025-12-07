package ua.terra.renderengine.renderer

import ua.terra.renderengine.RenderEngine
import ua.terra.renderengine.RenderType
import ua.terra.renderengine.text.*
import ua.terra.renderengine.text.fonts.FontHolder
import kotlin.math.round

/**
 * Renderer for drawing text using bitmap fonts.
 * Supports text alignment, positioning, rotation, and various text effects.
 */
class TextRenderer(private val engine: RenderEngine) {

    fun render(params: TextRenderParams) {
        render(
            params.text,
            params.x, params.y,
            params.fontSize,
            params.zIndex,
            params.horAlign, params.horPos,
            params.verAlign, params.verPos,
            params.color,
            params.angle,
            params.outlineThickness,
            params.ignoreZoom,
            params.ignoreCamera,
            params.font,
            params.effect
        )
    }

    fun render(
        text: String,
        x: Float, y: Float,
        fontSize: Int,
        zIndex: Int = 0,
        horAlign: TextHorAlignment = TextHorAlignment.LEFT,
        horPos: TextHorPosition = TextHorPosition.LEFT,
        verAlign: TextVerAlignment = TextVerAlignment.BOTTOM,
        verPos: TextVerPosition = TextVerPosition.BOTTOM,
        color: Int = -1,
        angle: Float = 0f,
        outlineThickness: Float = 6f,
        ignoreZoom: Boolean = true,
        ignoreCamera: Boolean = false,
        font: FontHolder,
        effect: TextEffect = TextEffect.DEFAULT
    ) {
        val font = font.font
        val scale = fontSize.toFloat() / font.size
        if (text.isBlank()) return

        var lineCount = 1
        var maxWidth = 0
        var currentLineWidth = 0

        for (char in text) {
            if (char == '\n') {
                lineCount++
                if (currentLineWidth > maxWidth) maxWidth = currentLineWidth
                currentLineWidth = 0
            } else {
                currentLineWidth += font.getCharacter(char).advanceWidth
            }
        }
        if (currentLineWidth > maxWidth) maxWidth = currentLineWidth

        val maxWidthScaled = maxWidth * scale
        val maxHeight = lineCount * font.lineHeight * scale

        val startX = x - round(maxWidthScaled * horPos.offset)
        val startY = y - round(maxHeight * verPos.offset)

        var lineIndex = 0
        var lineStartIdx = 0

        for (i in text.indices) {
            if (text[i] == '\n' || i == text.length - 1) {
                val lineEnd = if (text[i] == '\n') i else i + 1
                val line = text.substring(lineStartIdx, lineEnd)

                val lineWidth = getTextWidth(line, font) * scale
                val alignedX = startX + round((maxWidthScaled - lineWidth) * horAlign.offset)
                val alignedY = startY + round(lineIndex * font.size * scale * verAlign.offset)
                var cursorX = alignedX

                for (char in line) {
                    val charInfo = font.getCharacter(char)
                    engine.submitCommand(
                        font.imageFont.id,
                        cursorX + (charInfo.xOffset * scale),
                        alignedY - (charInfo.yOffset * scale),
                        charInfo.inAtlasWidth * scale,
                        charInfo.inAtlasHeight * scale,
                        charInfo.model.minU, charInfo.model.minV,
                        charInfo.model.maxU, charInfo.model.maxV,
                        zIndex,
                        color,
                        outlineThickness, angle,
                        ignoreZoom, ignoreCamera,
                        RenderType.TEXT.value,
                        effect.value
                    )
                    cursorX += charInfo.advanceWidth * scale
                }

                lineIndex++
                lineStartIdx = i + 1
            }
        }
    }

    fun renderText(
        text: String,
        x: Float, y: Float,
        fontSize: Int,
        color: Int = -1,
        font: FontHolder
    ) {
        render(
            text, x, y, fontSize,
            color = color,
            font = font
        )
    }

    private fun getTextWidth(text: String, font: FontHolder) = text.sumOf { font.font.getCharacter(it).advanceWidth }
}