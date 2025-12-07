package ua.terra.renderengine.text.fonts

import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTruetype.stbtt_GetFontVMetrics
import org.lwjgl.stb.STBTruetype.stbtt_ScaleForPixelHeight
import org.lwjgl.system.MemoryStack

class FontMetrics {
    val scale: Float
    val ascent: Int
    val descent: Int
    val lineGap: Int
    val lineHeight: Int

    constructor(scale: Float, ascent: Int, descent: Int, lineGap: Int) {
        this.scale = scale
        this.ascent = ascent
        this.descent = descent
        this.lineGap = lineGap
        lineHeight = ascent - descent + lineGap
    }

    constructor(fontInfo: STBTTFontinfo, size: Int) {
        scale = stbtt_ScaleForPixelHeight(fontInfo, size.toFloat())
        MemoryStack.stackPush().use { stack ->
            val pAscent = stack.mallocInt(1)
            val pDescent = stack.mallocInt(1)
            val pLineGap = stack.mallocInt(1)
            stbtt_GetFontVMetrics(fontInfo, pAscent, pDescent, pLineGap)
            ascent = (pAscent[0] * scale).toInt()
            descent = (pDescent[0] * scale).toInt()
            lineGap = (pLineGap[0] * scale).toInt()
        }
        lineHeight = ascent - descent + lineGap
    }
}