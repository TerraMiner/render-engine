package ua.terra.renderengine.sprite.config

import com.google.gson.annotations.SerializedName

/**
 * Serializable configuration that stays next to the texture (textureName.sprite.json).
 * If this file exists - texture is treated as animated sprite.
 */
data class SpriteConfig(
    @SerializedName("loop")
    val loop: Boolean = true,
    @SerializedName("smooth")
    val smooth: Boolean = false,
    @SerializedName("durationMs")
    val durationMs: Int = 120,
    @SerializedName("frames")
    val frames: List<SpriteFrameConfig> = emptyList()
)

