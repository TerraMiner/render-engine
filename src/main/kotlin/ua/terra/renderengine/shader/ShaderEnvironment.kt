package ua.terra.renderengine.shader

import org.lwjgl.opengl.GL20.*
import ua.terra.renderengine.resource.ResourceProvider
import java.io.File
import kotlin.system.exitProcess

/**
 * Represents a single shader stage (vertex or fragment).
 * Handles shader loading, compilation, and attachment to a program.
 * @property type OpenGL shader type (GL_VERTEX_SHADER or GL_FRAGMENT_SHADER)
 * @property fileName Name of the shader file
 */
class ShaderEnvironment(val type: Int, val fileName: String) {
    var id: Int = 0

    fun loadAndAttach(program: Int) {
        id = glCreateShader(type)

        val shaderPath = ResourceProvider.get().getResourcePath(fileName)
        val shaderSource = File(shaderPath).readText()

        glShaderSource(id, shaderSource)
        glCompileShader(id)

        if (glGetShaderi(id, GL_COMPILE_STATUS) != 1) {
            System.err.println("Shader compilation failed: $fileName")
            System.err.println(glGetShaderInfoLog(id))
            exitProcess(1)
        }
        glAttachShader(program, id)
    }
}