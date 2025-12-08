package ua.terra.renderengine.texture.source

import ua.terra.renderengine.renderer.TextureRenderer
import ua.terra.renderengine.sprite.SpriteDefinition
import ua.terra.renderengine.sprite.SpriteFrame
import ua.terra.renderengine.texture.model.Model

class AnimatedTexture(
    private val baseTexture: TextureHolder,
    private val definition: SpriteDefinition
) : TextureHolder by baseTexture {

    private var accumulator = 0f
    private var frameIndex = 0
    private var lastUpdateTime = System.currentTimeMillis()

    override val width: Int
        get() = currentFrame().width

    override val height: Int
        get() = currentFrame().height

    override val model: Model
        get() {
            updateAnimation()
            return currentFrame().model
        }

    override fun render(
        renderer: TextureRenderer,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        zIndex: Int,
        angle: Float,
        color: Int,
        thickness: Float,
        ignoreZoom: Boolean,
        ignoreCamera: Boolean
    ) {
        updateAnimation()

        val current = currentFrame().model
        val isSmooth = definition.smooth

        if (!isSmooth || definition.frames.size <= 1) {
            renderer.render(
                textureId,
                current,
                x, y, width, height,
                zIndex, angle, color,
                thickness, ignoreZoom, ignoreCamera
            )
            return
        }

        val nextIndex = if (frameIndex == definition.frames.lastIndex) {
            if (definition.loop) 0 else frameIndex
        } else {
            frameIndex + 1
        }

        val frameDuration = definition.frameDurationMs.toFloat()
        val blendFactor = if (frameDuration > 0) {
            (accumulator / frameDuration).coerceIn(0f, 1f)
        } else {
            0f
        }

        renderer.render(
            textureId,
            current,
            x, y, width, height,
            zIndex, angle, color,
            thickness, ignoreZoom, ignoreCamera
        )

        val originalAlpha = extractAlpha(color)
        val nextAlpha = (blendFactor * originalAlpha).toInt()
        val nextColor = setAlpha(color, nextAlpha)

        renderer.render(
            textureId,
            definition.frames[nextIndex].model,
            x, y, width, height,
            zIndex, angle, nextColor,
            thickness, ignoreZoom, ignoreCamera
        )
    }

    private fun currentFrame(): SpriteFrame = definition.frames[frameIndex]

    private fun updateAnimation() {
        if (definition.frames.size <= 1) return

        val currentTime = System.currentTimeMillis()
        val deltaMs = (currentTime - lastUpdateTime).toFloat()
        lastUpdateTime = currentTime

        accumulator += deltaMs
        val frameDuration = definition.frameDurationMs.toFloat()

        if (frameDuration <= 0) return

        while (accumulator >= frameDuration) {
            accumulator -= frameDuration
            frameIndex++

            if (frameIndex >= definition.frames.size) {
                if (definition.loop) {
                    frameIndex = 0
                } else {
                    frameIndex = definition.frames.lastIndex
                    accumulator = 0f
                    return
                }
            }
        }
    }

    private fun extractAlpha(color: Int): Int {
        return if (color == -1) {
            255
        } else {
            color and 0xFF
        }
    }

    private fun setAlpha(color: Int, alpha: Int): Int {
        val a = alpha.coerceIn(0, 255)
        return if (color == -1) {
            (0xFFFFFF shl 8) or a
        } else {
            (color and 0xFFFFFF00.toInt()) or a
        }
    }
}