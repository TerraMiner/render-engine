package ua.terra.renderengine.renderer

import ua.terra.renderengine.text.TextEffect
import ua.terra.renderengine.text.TextHorAlignment
import ua.terra.renderengine.text.TextHorPosition
import ua.terra.renderengine.text.TextVerAlignment
import ua.terra.renderengine.text.TextVerPosition
import ua.terra.renderengine.text.fonts.FontHolder

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