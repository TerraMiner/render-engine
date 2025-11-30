package ua.terra.renderengine.shader

import org.lwjgl.opengl.GL20.*
import kotlin.system.exitProcess

class ShaderEnvironment(val type: Int, val fileName: String) {
    var id: Int = 0
    
    fun loadAndAttach(program: Int) {
        id = glCreateShader(type)
        glShaderSource(id, load(fileName))
        glCompileShader(id)
        if (glGetShaderi(id, GL_COMPILE_STATUS) != 1) {
            System.err.println(glGetShaderInfoLog(id))
            exitProcess(1)
        }
        glAttachShader(program, id)
    }

    private fun load(path: String): String {
        return javaClass.classLoader.getResource(path.replace("resources/", ""))!!.readText()
    }
}