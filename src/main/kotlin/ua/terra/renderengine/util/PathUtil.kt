package ua.terra.renderengine.util

import java.io.File

object PathUtil {
    fun join(base: String, vararg parts: String): String {
        var result = base
        parts.forEach { part ->
            result = File(result, part).path
        }
        return result.replace('\\', '/')
    }

    fun normalize(path: String): String {
        return path.replace('\\', '/')
    }
}