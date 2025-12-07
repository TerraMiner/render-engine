package ua.terra.renderengine.texture.atlas

import ua.terra.renderengine.texture.model.Model

/**
 * Represents a texture atlas containing multiple textures packed into a single texture.
 * @property textureId The OpenGL texture ID of the atlas
 * @property width The atlas width in pixels
 * @property height The atlas height in pixels
 * @property regions Map of texture names to their regions in the atlas
 */
class TexturesAtlas(
    val textureId: Int,
    val width: Int,
    val height: Int,
    private val regions: Map<String, AtlasRegion>
) {
    /**
     * Gets the region for a texture by name.
     * @param name The texture name
     * @return The atlas region, or the default region if not found
     */
    fun getRegion(name: String): AtlasRegion =
        regions[name] ?: regions[""]!!

    /**
     * Gets the texture model (UV coordinates) for a texture by name.
     * @param name The texture name
     * @return The texture model
     */
    fun getModel(name: String): Model =
        getRegion(name).let { Model(it.minU, it.minV, it.maxU, it.maxV) }
}