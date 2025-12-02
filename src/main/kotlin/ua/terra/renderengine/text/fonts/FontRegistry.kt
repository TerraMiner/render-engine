package ua.terra.renderengine.text.fonts

import ua.terra.renderengine.RenderEngineCore

class FontRegistry {
    private var isInitialized = false
    private val fonts = LinkedHashMap<String, Font>()
    private val fontConfigs = LinkedHashMap<String, Pair<String, Int>>()

    fun register(key: String, path: String, size: Int) {
        if (isInitialized) {
            throw UnsupportedOperationException("Cannot register font $key! Fonts already initialized!")
        }
        if (fontConfigs.containsKey(key)) {
            throw UnsupportedOperationException("Cannot register font $key! Already registered")
        }

        fontConfigs[key] = Pair(path, size)
    }

    fun initialize() {
        if (isInitialized) {
            throw UnsupportedOperationException("Fonts already initialized!")
        }

        println("Initializing fonts...")//todo temporary with println, soon loading stages

        fontConfigs.forEach { (key, config) ->
            val (path, size) = config
            try {
                fonts[key] = Font.load(path, "${RenderEngineCore.getCoreApi().resourcesPath}/cache/fonts", size)
                println("Font '$key' loaded ($path, ${size}px)")
            } catch (e: Exception) {
                System.err.println("Failed to load font '$key': ${e.message}")
                throw IllegalStateException("Font initialization failed for '$key'", e)
            }
        }

        isInitialized = true
        println("Fonts initialized: ${fonts.size} fonts loaded")
    }

    fun reload(cachePath: String) {
        if (!isInitialized) {
            throw IllegalStateException("Cannot reload fonts - not initialized yet!")
        }

        println("Reloading fonts...")

        fonts.clear()

        fontConfigs.forEach { (key, config) ->
            val (path, size) = config
            try {
                fonts[key] = Font.load(path, cachePath, size)
                println("Font '$key' reloaded")
            } catch (e: Exception) {
                System.err.println("Failed to reload font '$key': ${e.message}")
            }
        }

        println("Fonts reloaded: ${fonts.size} fonts")
    }

    fun get(key: String): Font {
        if (!isInitialized) {
            throw IllegalStateException("Fonts not initialized yet! Call initialize() first.")
        }
        return fonts[key] ?: throw IllegalArgumentException("Font $key not registered")
    }

    fun getCharacter(fontKey: String, codepoint: Char): CharInfo {
        return get(fontKey).getCharacter(codepoint)
    }

    fun isLoaded(key: String): Boolean = fonts.containsKey(key)

    fun getAllKeys(): Set<String> = fonts.keys.toSet()
}