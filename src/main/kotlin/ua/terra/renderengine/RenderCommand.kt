package ua.terra.renderengine

/**
 * @param tId Texture Id
 *
 * x, y, w, h - object placement and size.
 *
 * uvX, uvY, uvMX, uvMY - rectangle for texture selection on atlas canvas.
 *
 * r, g, b, a - texture color-filter options.
 *
 * @param t thickness, extra parameter for different render types like Rectangle, Line etc.
 * @param rot rotation, for rotate object, doesn't work with Line render type.
 * @param iZ Ignores camera's zoom.
 * @param iC Ignores camera's offset.
 * @param rT Render type, specifies render type like Rectangle, Texture, Text etc.
 * @param zI Z-Index, realizes render order. Ascending
 *
 * sX, sY, sW, sH - Scissors, allows to ignore selected zone
 *
 * @param tE Texture Effect, like Rainbow, Chroma or Default
 */

class RenderCommand {
    var tId = 0
    var x = 0f
    var y = 0f
    var w = 0f
    var h = 0f
    var uvX = 0f
    var uvY = 0f
    var uvMX = 0f
    var uvMY = 0f
    var packedColor = 0
    var t = 0f
    var rot = 0f
    var iZ = 0f
    var iC = 0f
    var rT = 0f
    var zI = 0
    var sX = -1f
    var sY = 0f
    var sW = 0f
    var sH = 0f
    var tE = 0f
}