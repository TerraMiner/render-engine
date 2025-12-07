package ua.terra.renderengine.renderer

import ua.terra.renderengine.text.*
import ua.terra.renderengine.text.fonts.FontHolder

/**
 * Parameters for text rendering operations.
 * @property text The text to be rendered
 * @property x The x-coordinate of the text's anchor point
 * @property y The y-coordinate of the text's anchor point
 * @property fontSize The size of the font
 * @property zIndex The z-index for rendering order
 * @property horAlign The horizontal alignment of the text
 * @property horPos The horizontal position of the text
 * @property verAlign The vertical alignment of the text
 * @property verPos The vertical position of the text
 * @property color The text color
 * @property angle The rotation angle of the text
 * @property outlineThickness The thickness of the text outline
 * @property ignoreZoom Whether to ignore zoom level
 * @property ignoreCamera Whether to ignore camera position
 * @property font The font to be used for rendering the text
 * @property effect The visual effect to apply to the text
 */
data class TextRenderParams(
    val text: String,
    val x: Float,
    val y: Float,
    val fontSize: Int,
    val zIndex: Int = 0,
    val horAlign: TextHorAlignment = TextHorAlignment.LEFT,
    val horPos: TextHorPosition = TextHorPosition.LEFT,
    val verAlign: TextVerAlignment = TextVerAlignment.BOTTOM,
    val verPos: TextVerPosition = TextVerPosition.BOTTOM,
    val color: Int = -1,
    val angle: Float = 0f,
    val outlineThickness: Float = 6f,
    val ignoreZoom: Boolean = true,
    val ignoreCamera: Boolean = false,
    val font: FontHolder,
    val effect: TextEffect = TextEffect.DEFAULT
)