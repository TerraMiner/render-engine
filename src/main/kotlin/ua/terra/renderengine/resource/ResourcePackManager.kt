package ua.terra.renderengine.resource

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Resource pack management system like in Minecraft.
 *
 * Features:
 * - Load resources from JAR with caching
 * - Support custom resource packs next to executable
 * - Resource prioritization (higher in list = higher priority)
 * - JSON configuration for active packs
 *
 * Directory structure:
 * ```
 * game_directory/
 *   resourcepacks/
 *     MyPack/
 *       pack.json
 *       textures/
 *       shaders/
 *   cache/
 *     textures/
 *     fonts/
 *     extracted/
 *   resourcepacks.json
 * ```
 *
 * @param contextClass Game class for determining the correct directory (usually RenderEngineCore::class.java from the game)
 */
class ResourcePackManager(private val contextClass: Class<*>? = null) : ResourceProvider {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val gameDir: File by lazy { determineGameDirectory() }
    private val resourcePacksDir: File by lazy { File(gameDir, "resourcepacks").apply { mkdirs() } }
    private val cacheDir: File by lazy {
        File(gameDir, "cache").apply {
            mkdirs()
            File(this, "textures").mkdirs()
            File(this, "fonts").mkdirs()
            File(this, "extracted").mkdirs()
        }
    }
    private val configFile: File by lazy { File(gameDir, "resourcepacks.json") }

    private val activePacks = mutableListOf<ResourcePack>()
    private val availablePacks = mutableMapOf<String, ResourcePack>()
    private val resolvedPathCache = mutableMapOf<String, String>()
    private var initialized = false

    init {
        initialize()
    }

    /**
     * Initializes the resource pack system.
     */
    private fun initialize() {
        if (initialized) return
        initialized = true

        scanResourcePacks()
        loadConfig()

        println("ResourcePackManager initialized:")
        println("  Game directory: ${gameDir.absolutePath}")
        println("  ResourcePacks directory: ${resourcePacksDir.absolutePath}")
        println("  Cache directory: ${cacheDir.absolutePath}")
        println("  Available packs: ${availablePacks.size}")
        println("  Active packs: ${activePacks.map { it.id }}")
    }

    private fun determineGameDirectory(): File {
        val targetClass = contextClass ?: ResourcePackManager::class.java
        val codeSource = targetClass.protectionDomain.codeSource
        val jarFile = codeSource?.location?.toURI()?.let { File(it) }

        return when {
            jarFile != null && jarFile.isFile && jarFile.extension == "jar" -> {
                jarFile.parentFile ?: File(".")
            }

            jarFile != null && jarFile.isDirectory -> {
                jarFile.parentFile?.parentFile ?: File(".")
            }

            else -> {
                File(System.getProperty("user.dir"))
            }
        }.also {
            println("Game directory: ${it.absolutePath}")
            if (contextClass != null) {
                println("  (determined from context class: ${contextClass.simpleName})")
            }
        }
    }

    private fun scanResourcePacks() {
        availablePacks.clear()
        if (!resourcePacksDir.exists()) return

        resourcePacksDir.listFiles()?.filter { it.isDirectory }?.forEach { packDir ->
            val packJsonFile = File(packDir, "pack.json")
            if (!packJsonFile.exists()) return

            try {
                val metadata = gson.fromJson(packJsonFile.readText(), ResourcePackMetadata::class.java)
                val pack = ResourcePack(packDir.name, packDir, metadata)
                availablePacks[pack.id] = pack
                println("Found resource pack: ${pack.id} - ${pack.metadata.name}")
            } catch (e: Exception) {
                println("Error: Failed to parse pack.json in ${packDir.name}: ${e.message}")
            }
        }
    }

    private fun loadConfig() {
        activePacks.clear()
        if (configFile.exists()) {
            try {
                val config = gson.fromJson(configFile.readText(), ResourcePackConfig::class.java)
                config.activePacks.forEach { packId ->
                    availablePacks[packId]?.let { activePacks.add(it) }
                }
            } catch (e: Exception) {
                println("Warning: Failed to load resourcepacks.json: ${e.message}")
            }
        }
        saveConfig()
    }

    fun saveConfig() {
        try {
            configFile.writeText(gson.toJson(ResourcePackConfig(activePacks.map { it.id })))
        } catch (e: Exception) {
            println("Warning: Failed to save resourcepacks.json: ${e.message}")
        }
    }

    fun clearCache() {
        resolvedPathCache.clear()
    }

    override fun getResourcePath(resourcePath: String): String {

        val absoluteFile = File(resourcePath)
        if (absoluteFile.isAbsolute && absoluteFile.exists() && absoluteFile.isFile) {
            return absoluteFile.absolutePath
        }

        val normalizedPath = resourcePath.replace('\\', '/').removePrefix("/")

        resolvedPathCache[normalizedPath]?.let { cached ->
            if (File(cached).exists()) return cached
        }

        for (pack in activePacks) {
            val packFile = File(pack.directory, normalizedPath)
            if (packFile.exists() && packFile.isFile) {
                resolvedPathCache[normalizedPath] = packFile.absolutePath
                return packFile.absolutePath
            }
        }

        return extractNativeResource(normalizedPath)
    }

    private fun extractNativeResource(resourcePath: String): String {
        val devFile = File("src/main/resources", resourcePath)
        if (devFile.exists()) {
            resolvedPathCache[resourcePath] = devFile.absolutePath
            return devFile.absolutePath
        }

        val cacheFile = File(cacheDir, "extracted/$resourcePath")
        if (cacheFile.exists()) {
            resolvedPathCache[resourcePath] = cacheFile.absolutePath
            return cacheFile.absolutePath
        }

        val resourceStream = javaClass.getResourceAsStream("/$resourcePath")
            ?: throw IllegalArgumentException("Resource not found: $resourcePath")

        return extractToCache(resourceStream, resourcePath, cacheFile)
    }

    private fun extractToCache(inputStream: InputStream, resourcePath: String, cacheFile: File): String {
        cacheFile.parentFile.mkdirs()
        inputStream.use { input ->
            FileOutputStream(cacheFile).use { output ->
                input.copyTo(output)
            }
        }
        resolvedPathCache[resourcePath] = cacheFile.absolutePath
        return cacheFile.absolutePath
    }

    override fun resourceExists(resourcePath: String): Boolean {
        val absoluteFile = File(resourcePath)
        if (absoluteFile.isAbsolute && absoluteFile.exists() && absoluteFile.isFile) {
            return true
        }

        val normalizedPath = resourcePath.replace('\\', '/').removePrefix("/")

        for (pack in activePacks) {
            if (File(pack.directory, normalizedPath).exists()) return true
        }
        if (File("src/main/resources", normalizedPath).exists()) return true

        return javaClass.getResourceAsStream("/$normalizedPath")?.also { it.close() } != null
    }

    override fun getCachePath(): String {
        return cacheDir.absolutePath.replace('\\', '/')
    }

    fun getAvailablePacks(): List<ResourcePack> {
        return availablePacks.values.toList()
    }

    fun getActivePacks(): List<ResourcePack> {
        return activePacks.toList()
    }

    fun enablePack(packId: String) {
        val pack = availablePacks[packId] ?: return
        if (pack !in activePacks) {
            activePacks.add(pack)
            clearCache()
            saveConfig()
        }
    }

    fun disablePack(packId: String) {
        activePacks.removeIf { it.id == packId }
        clearCache()
        saveConfig()
    }

    fun movePackUp(packId: String) {
        val index = activePacks.indexOfFirst { it.id == packId }
        if (index > 0) {
            val pack = activePacks.removeAt(index)
            activePacks.add(index - 1, pack)
            clearCache()
            saveConfig()
        }
    }

    fun movePackDown(packId: String) {
        val index = activePacks.indexOfFirst { it.id == packId }
        if (index in 0 until activePacks.size - 1) {
            val pack = activePacks.removeAt(index)
            activePacks.add(index + 1, pack)
            clearCache()
            saveConfig()
        }
    }

    fun setActivePacksOrder(packIds: List<String>) {
        activePacks.clear()
        packIds.forEach { packId ->
            availablePacks[packId]?.let { activePacks.add(it) }
        }
        clearCache()
        saveConfig()
    }

    fun rescan() {
        val previousActive = activePacks.map { it.id }
        scanResourcePacks()
        activePacks.clear()
        previousActive.forEach { packId ->
            availablePacks[packId]?.let { activePacks.add(it) }
        }
        clearCache()
        saveConfig()
    }

    fun getCacheDirectory(): File {
        return cacheDir
    }

    fun getResourcePacksDirectory(): File {
        return resourcePacksDir
    }

    fun getGameDirectory(): File {
        return gameDir
    }
}