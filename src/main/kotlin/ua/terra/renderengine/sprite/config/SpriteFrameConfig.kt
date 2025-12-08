package ua.terra.renderengine.sprite.config

import com.google.gson.annotations.SerializedName

/**
 * Frame definition expressed in pixels relative to the original texture.
 */
data class SpriteFrameConfig(
    @SerializedName("x")
    val x: Int = 0,
    @SerializedName("y")
    val y: Int = 0,
    @SerializedName("width")
    val width: Int = 0,
    @SerializedName("height")
    val height: Int = 0
)