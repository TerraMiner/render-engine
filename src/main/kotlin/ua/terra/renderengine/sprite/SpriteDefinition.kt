package ua.terra.renderengine.sprite

/**
 * Aggregated sprite description retrieved from resource config.
 * If frames.size > 1 - it's an animation.
 */
data class SpriteDefinition(
    val texturePath: String,
    val frames: List<SpriteFrame>,
    val loop: Boolean,
    val smooth: Boolean,
    val frameDurationMs: Int
) {
    val isAnimated: Boolean get() = frames.size > 1
}

