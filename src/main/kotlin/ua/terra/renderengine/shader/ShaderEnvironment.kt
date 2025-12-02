package ua.terra.renderengine.shader

import org.lwjgl.opengl.GL20.*
import ua.terra.renderengine.RenderEngineCore
import ua.terra.renderengine.util.PathUtil
import java.io.File
import kotlin.system.exitProcess

class ShaderEnvironment(val type: Int, val fileName: String) {
    var id: Int = 0

    fun loadAndAttach(program: Int) {
        id = glCreateShader(type)

        val shaderPath = PathUtil.join(
            RenderEngineCore.getCoreApi().resourcesPath,
            fileName
        )

        val shaderSource = if (File(shaderPath).exists()) {
            File(shaderPath).readText()
        } else {
            javaClass.getResourceAsStream("/$fileName")?.bufferedReader()?.readText()
                ?: throw IllegalStateException("Shader not found: $fileName (not in $shaderPath and not in classpath)")
        }

        glShaderSource(id, shaderSource)
        glCompileShader(id)

        if (glGetShaderi(id, GL_COMPILE_STATUS) != 1) {
            System.err.println(glGetShaderInfoLog(id))
            exitProcess(1)
        }
        glAttachShader(program, id)
    }
}