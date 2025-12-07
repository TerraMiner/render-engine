package ua.terra.renderengine.resource

/**
 * Interface for application resource loading.
 *
 * Allows flexible resource loading system with support for:
 * - Resource packs (like in Minecraft)
 * - Fallback to native JAR resources
 * - Caching and optimization
 */
interface ResourceProvider {
    /**
     * Returns absolute path to resource on filesystem.
     *
     * @param resourcePath Relative path (e.g. "textures/block/stone.png")
     * @return Absolute path to resource file
     * @throws IllegalArgumentException if resource not found
     */
    fun getResourcePath(resourcePath: String): String

    /**
     * Returns path to cache directory for atlases and processing.
     */
    fun getCachePath(): String

    /**
     * Checks if resource exists.
     */
    fun resourceExists(resourcePath: String): Boolean

    companion object {
        private var instance: ResourceProvider? = null

        /**
         * Registers global resource provider.
         * Must be called BEFORE RenderEngineCore initialization.
         */
        fun register(provider: ResourceProvider) {
            instance = provider
        }

        /**
         * Returns current resource provider.
         * If not registered, automatically creates ResourcePackManager.
         */
        fun get(): ResourceProvider {
            if (instance == null) {
                instance = ResourcePackManager()
            }
            return instance!!
        }

        /**
         * Checks if provider is registered.
         */
        fun isRegistered(): Boolean = instance != null
    }
}