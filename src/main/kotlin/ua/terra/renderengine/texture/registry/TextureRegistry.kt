package ua.terra.renderengine.texture.registry

import org.lwjgl.opengl.GL11.glDeleteTextures
import org.lwjgl.system.MemoryUtil
import ua.terra.renderengine.resource.ResourceProvider
import ua.terra.renderengine.sprite.SpriteConfigLoader
import ua.terra.renderengine.texture.atlas.AtlasTexture
import ua.terra.renderengine.texture.atlas.TextureAtlasBuilder
import ua.terra.renderengine.texture.atlas.TexturesAtlas
import ua.terra.renderengine.texture.manager.RawTexture
import ua.terra.renderengine.texture.source.AnimatedTexture
import ua.terra.renderengine.texture.source.SingleTexture
import ua.terra.renderengine.texture.source.TextureHolder
import ua.terra.renderengine.util.DEFAULT_ATLAS_PADDING

/**
 * Registry for managing textures, both as individual textures and within atlases.
 * Provides texture registration, atlas building, and texture access.
 */
class TextureRegistry {
    private lateinit var atlas: TexturesAtlas

    private var isBuilt = false
    private val registeredPaths = LinkedHashSet<String>()
    private val textureCache = mutableMapOf<String, TextureHolder>()

    private var geometryBlankTexture: RawTexture? = null

    fun registerForAtlas(path: String) {
        if (isBuilt) {
            throw UnsupportedOperationException("Cannot register texture $path! Atlas already built!")
        }
        if (registeredPaths.contains(path)) {
            throw UnsupportedOperationException("Cannot register texture $path! Already registered")
        }

        registeredPaths.add(path)
    }

    fun getSingle(path: String): TextureHolder {
        return SingleTexture(path)
    }

    fun getFromAtlas(path: String): TextureHolder {
        check(isBuilt) { "Atlas not built yet! Call buildAtlas() first." }
        return textureCache[path] ?: throw IllegalArgumentException("Texture $path not registered in atlas")
    }

    fun getBlank(): RawTexture {
        return geometryBlankTexture ?: createGeometryBlankTexture().also {
            geometryBlankTexture = it
        }
    }

    private fun createGeometryBlankTexture(): RawTexture {
        val buffer = MemoryUtil.memAlloc(4).apply {
            put(255.toByte()).put(255.toByte()).put(255.toByte()).put(255.toByte())
            flip()
        }

        val texture = RawTexture(1, 1, buffer)
        MemoryUtil.memFree(buffer)

        return texture
    }

    fun buildAtlas(padding: Int = DEFAULT_ATLAS_PADDING) {
        if (isBuilt) {
            throw UnsupportedOperationException("Atlas already built!")
        }

        defineAtlasAndPaths(padding)

        isBuilt = true
        println("Texture atlas created: ${atlas.width}x${atlas.height}, textures: ${registeredPaths.size + 1}")
    }

    fun reload(padding: Int = DEFAULT_ATLAS_PADDING) {
        check(isBuilt) { "Cannot reload atlas - not built yet!" }

        println("Reloading texture atlas...")

        glDeleteTextures(atlas.textureId)
        textureCache.clear()

        defineAtlasAndPaths(padding)

        println("Texture atlas reloaded: ${atlas.width}x${atlas.height}, textures: ${registeredPaths.size + 1}")
    }

    /**
     * Get texture by path.
     */
    operator fun get(path: String): TextureHolder {
        return if (isBuilt && registeredPaths.contains(path)) {
            getFromAtlas(path)
        } else {
            getSingle(path)
        }
    }

    fun isRegistered(path: String): Boolean = registeredPaths.contains(path)

    fun getAllRegistered(): Set<String> = registeredPaths.toSet()

    private fun defineAtlasAndPaths(padding: Int) {
        val cachePath = "${ResourceProvider.get().getCachePath()}/textures"

        atlas = TextureAtlasBuilder.build(registeredPaths, cachePath, padding)

        registeredPaths.forEach { path ->
            val baseTexture = AtlasTexture(atlas, path)

            val config = SpriteConfigLoader.loadConfig(path)
            if (config != null && config.frames.isNotEmpty()) {
                val definition = SpriteConfigLoader.createDefinitionForAtlas(path, atlas, config)
                if (definition.isAnimated) {
                    textureCache[path] = AnimatedTexture(baseTexture, definition)
                } else {
                    textureCache[path] = baseTexture
                }
            } else {
                textureCache[path] = baseTexture
            }
        }
    }

    companion object {
        internal const val BLANK_TEXTURE_PATH = ""
    }
}