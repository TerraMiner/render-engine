package ua.terra.renderengine.util

class Color private constructor(val rgba: Int) {

    constructor(r: Int, g: Int, b: Int) : this(r, g, b, 255)
    constructor(r: Int, g: Int, b: Int, a: Int) : this((r shl 24) or (g shl 16) or (b shl 8) or a)

    constructor(r: Float, g: Float, b: Float) : this(r, g, b, 1.0f)
    constructor(r: Float, g: Float, b: Float, a: Float) : this(
        (r * 255).toInt().coerceIn(0, 255),
        (g * 255).toInt().coerceIn(0, 255),
        (b * 255).toInt().coerceIn(0, 255),
        (a * 255).toInt().coerceIn(0, 255)
    )

    val r: Float by lazy { ((rgba shr 24) and 0xFF) / 255.0f }
    val g: Float by lazy { ((rgba shr 16) and 0xFF) / 255.0f }
    val b: Float by lazy { ((rgba shr 8) and 0xFF) / 255.0f }
    val a: Float by lazy { (rgba and 0xFF) / 255.0f }

    val ri: Int by lazy { ((rgba shr 24) and 0xFF) }
    val gi: Int by lazy { ((rgba shr 16) and 0xFF) }
    val bi: Int by lazy { ((rgba shr 8) and 0xFF) }
    val ai: Int by lazy { (rgba and 0xFF) }

    fun toInt(): Int = rgba

    companion object {
        val BLACK = Color(0x000000FF)
        val DARK_BLUE = Color(0x0000AAFF)
        val DARK_GREEN = Color(0x00AA00FF)
        val DARK_AQUA = Color(0x00AAAAFF)
        val DARK_RED = Color(0xAA0000FF.toInt())
        val DARK_PURPLE = Color(0xAA00AAFF.toInt())
        val GOLD = Color(0xFFAA00FF.toInt())
        val GRAY = Color(0xAAAAAAFF.toInt())
        val DARK_GRAY = Color(0x555555FF)
        val BLUE = Color(0x5555FFFF)
        val GREEN = Color(0x55FF55FF)
        val AQUA = Color(0x55FFFFFF)
        val RED = Color(0xFF5555FF.toInt())
        val LIGHT_PURPLE = Color(0xFF55FFFF.toInt())
        val YELLOW = Color(0xFFFF55FF.toInt())
        val WHITE = Color(0xFFFFFFFF.toInt())
    }
}