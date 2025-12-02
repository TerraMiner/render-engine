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

sealed class RenderCommand {
    abstract var tId: Int
    abstract var x: Float
    abstract var y: Float
    abstract var w: Float
    abstract var h: Float
    abstract var uvX: Float
    abstract var uvY: Float
    abstract var uvMX: Float
    abstract var uvMY: Float
    abstract var packedColor: Int
    abstract var t: Float
    abstract var rot: Float
    abstract var iZ: Float
    abstract var iC: Float
    abstract var rT: Float
    abstract var zI: Int
    abstract var sX: Float
    abstract var sY: Float
    abstract var sW: Float
    abstract var sH: Float
    abstract var tE: Float

    class TextureCommand : RenderCommand() {
        override var tId = 0
        override var x = 0f
        override var y = 0f
        override var w = 0f
        override var h = 0f
        override var uvX = 0f
        override var uvY = 0f
        override var uvMX = 0f
        override var uvMY = 0f
        override var packedColor = 0
        override var t = 0f
        override var rot = 0f
        override var iZ = 0f
        override var iC = 0f
        override var rT = RenderType.TEXTURE.value
        override var zI = 0
        override var sX = -1f
        override var sY = 0f
        override var sW = 0f
        override var sH = 0f
        override var tE = 0f
    }

    class TextCommand : RenderCommand() {
        override var tId = 0
        override var x = 0f
        override var y = 0f
        override var w = 0f
        override var h = 0f
        override var uvX = 0f
        override var uvY = 0f
        override var uvMX = 0f
        override var uvMY = 0f
        override var packedColor = 0
        override var t = 0f
        override var rot = 0f
        override var iZ = 0f
        override var iC = 0f
        override var rT = RenderType.TEXT.value
        override var zI = 0
        override var sX = -1f
        override var sY = 0f
        override var sW = 0f
        override var sH = 0f
        override var tE = 0f
    }

    class GeometryCommand : RenderCommand() {
        override var tId = 0
        override var x = 0f
        override var y = 0f
        override var w = 0f
        override var h = 0f
        override var uvX = 0f
        override var uvY = 0f
        override var uvMX = 0f
        override var uvMY = 0f
        override var packedColor = 0
        override var t = 0f
        override var rot = 0f
        override var iZ = 0f
        override var iC = 0f
        override var rT = RenderType.FILL_RECT.value
        override var zI = 0
        override var sX = -1f
        override var sY = 0f
        override var sW = 0f
        override var sH = 0f
        override var tE = 0f
    }
}