package ua.terra.renderengine.sprite

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import ua.terra.renderengine.resource.ResourceProvider
import ua.terra.renderengine.sprite.config.SpriteConfig
import ua.terra.renderengine.sprite.config.SpriteFrameConfig
import ua.terra.renderengine.texture.atlas.AtlasRegion
import ua.terra.renderengine.texture.atlas.TexturesAtlas
import ua.terra.renderengine.texture.model.Model
import java.io.File

/**
 * Sprite configuration loader.
 * Searches for .sprite.json files next to textures and creates SpriteDefinition.
 */
object SpriteConfigLoader {

    private val gson: Gson = GsonBuilder().create()

    /**
     * Load sprite configuration for a texture.
     * @param texturePath Path to texture (e.g. "textures/player.png")
     * @return SpriteConfig or null if config not found
     */
    fun loadConfig(texturePath: String): SpriteConfig? {
        val configPath = texturePath.replaceAfterLast('.', "sprite.json")

        return try {
            if (!ResourceProvider.get().resourceExists(configPath)) {
                return null
            }
            val configFile = File(ResourceProvider.get().getResourcePath(configPath))
            if (!configFile.exists()) return null

            gson.fromJson(configFile.readText(), SpriteConfig::class.java)
        } catch (e: Exception) {
            System.err.println("Failed to load sprite config for $texturePath: ${e.message}")
            null
        }
    }

    /**
     * Create SpriteDefinition from config and texture (without atlas).
     * @param texturePath Path to texture
     * @param textureWidth Texture width in pixels
     * @param textureHeight Texture height in pixels
     * @param config Sprite configuration (if null — creates static sprite for entire texture)
     */
    fun createDefinition(
        texturePath: String,
        textureWidth: Int,
        textureHeight: Int,
        config: SpriteConfig?
    ): SpriteDefinition {
        if (config == null || config.frames.isEmpty()) {
            // Static sprite — entire texture as one frame
            val frame = SpriteFrame(
                width = textureWidth,
                height = textureHeight,
                model = Model.DEFAULT
            )
            return SpriteDefinition(
                texturePath = texturePath,
                frames = listOf(frame),
                loop = false,
                smooth = false,
                frameDurationMs = 0
            )
        }

        val frames = config.frames.map { frameConfig ->
            frameConfigToFrame(frameConfig, textureWidth, textureHeight)
        }

        return SpriteDefinition(
            texturePath = texturePath,
            frames = frames,
            loop = config.loop,
            smooth = config.smooth,
            frameDurationMs = config.durationMs
        )
    }

    /**
     * Create SpriteDefinition with coordinate projection to atlas.
     * Frame coordinates from config are recalculated relative to texture position in atlas.
     *
     * @param texturePath Path to texture
     * @param atlas Texture atlas
     * @param config Sprite configuration
     */
    fun createDefinitionForAtlas(
        texturePath: String,
        atlas: TexturesAtlas,
        config: SpriteConfig?
    ): SpriteDefinition {
        val region = atlas.getRegion(texturePath)

        if (config == null || config.frames.isEmpty()) {
            val frame = SpriteFrame(
                width = region.pixelWidth,
                height = region.pixelHeight,
                model = Model(region.minU, region.minV, region.maxU, region.maxV)
            )
            return SpriteDefinition(
                texturePath = texturePath,
                frames = listOf(frame),
                loop = false,
                smooth = false,
                frameDurationMs = 0
            )
        }

        val frames = config.frames.map { frameConfig ->
            frameConfigToAtlasFrame(frameConfig, region, atlas)
        }

        return SpriteDefinition(
            texturePath = texturePath,
            frames = frames,
            loop = config.loop,
            smooth = config.smooth,
            frameDurationMs = config.durationMs
        )
    }

    /**
     * Convert frame config to SpriteFrame for regular texture.
     */
    private fun frameConfigToFrame(
        config: SpriteFrameConfig,
        textureWidth: Int,
        textureHeight: Int
    ): SpriteFrame {
        val minU = config.x.toFloat() / textureWidth
        val minV = config.y.toFloat() / textureHeight
        val maxU = (config.x + config.width).toFloat() / textureWidth
        val maxV = (config.y + config.height).toFloat() / textureHeight

        return SpriteFrame(
            width = config.width,
            height = config.height,
            model = Model(minU, minV, maxU, maxV)
        )
    }

    /**
     * Convert frame config to SpriteFrame with atlas projection.
     * Coordinates from config (relative to original texture) are converted
     * to absolute atlas coordinates.
     */
    private fun frameConfigToAtlasFrame(
        config: SpriteFrameConfig,
        textureRegion: AtlasRegion,
        atlas: TexturesAtlas
    ): SpriteFrame {
        val atlasX = textureRegion.pixelX + config.x
        val atlasY = textureRegion.pixelY + config.y

        val atlasWidth = atlas.width.toFloat()
        val atlasHeight = atlas.height.toFloat()

        val minU = atlasX / atlasWidth
        val minV = atlasY / atlasHeight
        val maxU = (atlasX + config.width) / atlasWidth
        val maxV = (atlasY + config.height) / atlasHeight

        return SpriteFrame(
            width = config.width,
            height = config.height,
            model = Model(minU, minV, maxU, maxV)
        )
    }
}

