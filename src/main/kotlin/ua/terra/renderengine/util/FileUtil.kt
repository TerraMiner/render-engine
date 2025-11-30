package ua.terra.renderengine.util

import java.io.File
import java.io.FileOutputStream


object FileUtil {
    fun getPath(path: String, resFolderName: String = "resources"): String = runCatching {
        val cleanPath = if (path.startsWith("$resFolderName/")) path.substring(4) else path
        val resourceStream = javaClass.getResourceAsStream("/${cleanPath}")
            ?: throw IllegalArgumentException("Resource not found: /${cleanPath}")

        val tempFile = File.createTempFile("texture", ".png")
        tempFile.deleteOnExit()

        resourceStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        tempFile.absolutePath
    }.getOrElse { path }
}