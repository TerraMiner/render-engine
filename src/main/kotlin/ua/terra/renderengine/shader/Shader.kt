package ua.terra.renderengine.shader

import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL20.*
import ua.terra.renderengine.window.Window
import java.nio.FloatBuffer
import kotlin.system.exitProcess

abstract class Shader(val fileName: String) {
    protected var program: Int = 0

    protected val vertexShader = ShaderEnvironment(GL_VERTEX_SHADER, "$fileName.vert")
    protected val fragmentShader = ShaderEnvironment(GL_FRAGMENT_SHADER, "$fileName.frag")

    protected var projectionLoc: Int = 0
    private var isRegistered = false
    protected val projectionBuffer: FloatBuffer = BufferUtils.createFloatBuffer(16)

    protected abstract fun build()

    fun register() {
        if (isRegistered) {
            println("Shader $fileName already registered!")
            return
        }
        program = glCreateProgram()
        isRegistered = true
        build()
    }

    fun reload() {
        if (!isRegistered) {
            throw IllegalStateException("Cannot reload shader $fileName - not registered yet!")
        }

        glDeleteProgram(program)

        program = glCreateProgram()
        build()

        println("Shader $fileName reloaded successfully")
    }

    protected fun ensureRegistered() {
        if (!isRegistered) {
            throw IllegalStateException("Shader $fileName is not registered. Call register() first.")
        }
    }

    fun loadVertexShader() {
        vertexShader.loadAndAttach(program)
    }

    fun loadFragmentShader() {
        fragmentShader.loadAndAttach(program)
    }

    fun linkAndValidateProgram() {
        glLinkProgram(program)
        if (glGetProgrami(program, GL_LINK_STATUS) != 1) {
            System.err.println(glGetProgramInfoLog(program))
            exitProcess(1)
        }
        glValidateProgram(program)
        if (glGetProgrami(program, GL_VALIDATE_STATUS) != 1) {
            System.err.println(glGetProgramInfoLog(program))
            exitProcess(1)
        }
    }

    fun freeShaderResources() {
        glDeleteShader(vertexShader.id)
        glDeleteShader(fragmentShader.id)
    }

    fun setProjection(value: Matrix4f) {
        ensureRegistered()
        projectionBuffer.clear()
        value.get(projectionBuffer)
        glUniformMatrix4fv(projectionLoc, false, projectionBuffer)
    }

    fun bind() {
        ensureRegistered()
        if (activeShaderProgram == program) return
        glUseProgram(program)
        activeShaderProgram = program
    }

    companion object {
        var activeShaderProgram: Int = -1
    }
}