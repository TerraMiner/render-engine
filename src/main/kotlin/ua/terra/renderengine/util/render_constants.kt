package ua.terra.renderengine.util

/** Maximum number of sprites that can be rendered in a single batch */
const val MAX_SPRITES = 128000

/** Number of float values per sprite in the vertex data */
const val FLOATS_PER_SPRITE = 19

/** Size of sprite data in bytes */
const val FLOATS_PER_SPRITE_BYTES = FLOATS_PER_SPRITE * Float.SIZE_BYTES

/** Total buffer size in floats */
const val BUFFER_SIZE = MAX_SPRITES * FLOATS_PER_SPRITE

/** Total buffer size in bytes */
const val BUFFER_SIZE_BYTES = BUFFER_SIZE * Float.SIZE_BYTES

/** Default padding between textures in the texture atlas */
const val DEFAULT_ATLAS_PADDING = 2

/** Maximum size of the texture atlas in pixels */
const val MAX_ATLAS_SIZE = 8192

/** Minimum window width in pixels */
const val MIN_WINDOW_WIDTH = 800

/** Minimum window height in pixels */
const val MIN_WINDOW_HEIGHT = 600

/** Default maximum frames per second */
const val DEFAULT_MAX_FPS = 240