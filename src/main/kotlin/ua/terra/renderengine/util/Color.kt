package ua.terra.renderengine.util

/**
 * Represents a color with red, green, blue, and alpha components.
 * Stores color as a packed 32-bit RGBA integer internally for efficient memory usage.
 *
 * Color format: RGBA (Red-Green-Blue-Alpha)
 * - Red: bits 24-31
 * - Green: bits 16-23
 * - Blue: bits 8-15
 * - Alpha: bits 0-7
 *
 * @property rgba The packed RGBA integer value
 */
class Color private constructor(val rgba: Int) {

    /** Creates a Color from 0-255 integer RGB values with full opacity */
    constructor(r: Int, g: Int, b: Int) : this(r, g, b, 255)

    /** Creates a Color from 0-255 integer RGBA values */
    constructor(r: Int, g: Int, b: Int, a: Int) : this((r shl 24) or (g shl 16) or (b shl 8) or a)

    /** Creates a Color from 0.0-1.0 float RGB values with full opacity */
    constructor(r: Float, g: Float, b: Float) : this(r, g, b, 1.0f)

    /** Creates a Color from 0.0-1.0 float RGBA values */
    constructor(r: Float, g: Float, b: Float, a: Float) : this(
        (r * 255).toInt().coerceIn(0, 255),
        (g * 255).toInt().coerceIn(0, 255),
        (b * 255).toInt().coerceIn(0, 255),
        (a * 255).toInt().coerceIn(0, 255)
    )

    /** Red component as float (0.0 to 1.0) */
    val r: Float by lazy { ((rgba shr 24) and 0xFF) / 255.0f }

    /** Green component as float (0.0 to 1.0) */
    val g: Float by lazy { ((rgba shr 16) and 0xFF) / 255.0f }

    /** Blue component as float (0.0 to 1.0) */
    val b: Float by lazy { ((rgba shr 8) and 0xFF) / 255.0f }

    /** Alpha component as float (0.0 to 1.0) */
    val a: Float by lazy { (rgba and 0xFF) / 255.0f }

    /** Red component as integer (0 to 255) */
    val ri: Int by lazy { ((rgba shr 24) and 0xFF) }

    /** Green component as integer (0 to 255) */
    val gi: Int by lazy { ((rgba shr 16) and 0xFF) }

    /** Blue component as integer (0 to 255) */
    val bi: Int by lazy { ((rgba shr 8) and 0xFF) }

    /** Alpha component as integer (0 to 255) */
    val ai: Int by lazy { (rgba and 0xFF) }

    /**
     * Returns the packed RGBA integer value.
     * @return Packed color as integer
     */
    fun toInt(): Int = rgba

    companion object {
        /** Pure black color (0, 0, 0) */
        val BLACK = Color(0x000000FF)

        /** Dark blue color (0, 0, 170) */
        val DARK_BLUE = Color(0x0000AAFF)

        /** Dark green color (0, 170, 0) */
        val DARK_GREEN = Color(0x00AA00FF)

        /** Dark cyan/aqua color (0, 170, 170) */
        val DARK_AQUA = Color(0x00AAAAFF)

        /** Dark red color (170, 0, 0) */
        val DARK_RED = Color(0xAA0000FF.toInt())

        /** Dark purple/magenta color (170, 0, 170) */
        val DARK_PURPLE = Color(0xAA00AAFF.toInt())

        /** Gold/orange color (255, 170, 0) */
        val GOLD = Color(0xFFAA00FF.toInt())

        /** Gray color (170, 170, 170) */
        val GRAY = Color(0xAAAAAAFF.toInt())

        /** Dark gray color (85, 85, 85) */
        val DARK_GRAY = Color(0x555555FF)

        /** Bright blue color (85, 85, 255) */
        val BLUE = Color(0x5555FFFF)

        /** Bright green color (85, 255, 85) */
        val GREEN = Color(0x55FF55FF)

        /** Bright cyan/aqua color (85, 255, 255) */
        val AQUA = Color(0x55FFFFFF)

        /** Bright red color (255, 85, 85) */
        val RED = Color(0xFF5555FF.toInt())

        /** Light purple/magenta color (255, 85, 255) */
        val LIGHT_PURPLE = Color(0xFF55FFFF.toInt())

        /** Yellow color (255, 255, 85) */
        val YELLOW = Color(0xFFFF55FF.toInt())

        /** Pure white color (255, 255, 255) */
        val WHITE = Color(0xFFFFFFFF.toInt())
    }
}