package ua.terra.renderengine.shader

import org.joml.Matrix4f
import org.lwjgl.opengl.GL20.*
import ua.terra.renderengine.window.Window

class BatchShader : Shader("shaders/batch") {
    private var colorLoc: Int = 0
    private var samplerLoc: Int = 0
    private var cameraScaleLoc: Int = 0
    private var cameraCenterLoc: Int = 0
    private var cameraOffsetLoc: Int = 0
    private var timeLocation = 0

    override fun build(window: Window) {
        loadVertexShader()
        loadFragmentShader()

        glBindAttribLocation(program, 0, "rectData")
        glBindAttribLocation(program, 1, "uvData")
        glBindAttribLocation(program, 2, "colorData")
        glBindAttribLocation(program, 3, "transformData")
        glBindAttribLocation(program, 4, "ignoreZoom")
        glBindAttribLocation(program, 5, "ignoreCamera")
        glBindAttribLocation(program, 6, "renderType")
        glBindAttribLocation(program, 7, "scissorRect")

        linkAndValidateProgram()
        freeShaderResources()

        samplerLoc = glGetUniformLocation(program, "sampler")
        projectionLoc = glGetUniformLocation(program, "projection")
        colorLoc = glGetUniformLocation(program, "color")
        cameraScaleLoc = glGetUniformLocation(program, "cameraScale")
        cameraCenterLoc = glGetUniformLocation(program, "cameraCenter")
        cameraOffsetLoc = glGetUniformLocation(program, "cameraOffset")
        timeLocation = glGetUniformLocation(program, "time")

        bind()
        glUniform1i(samplerLoc, 0)
        setColor(1f, 1f, 1f, 1f)
        setCameraScale(1f, 1f)
        setCameraCenter(window.centerX, window.centerY)
        setProjection(Matrix4f().setOrtho2D(0f, window.width.toFloat(), window.height.toFloat(), 0f))
    }

    fun setColor(r: Float, g: Float, b: Float, a: Float) {
        glUniform4f(colorLoc, r, g, b, a)
    }

    fun setCameraScale(scaleX: Float, scaleY: Float) {
        glUniform2f(cameraScaleLoc, scaleX, scaleY)
    }

    fun setCameraCenter(x: Float, y: Float) {
        glUniform2f(cameraCenterLoc, x, y)
    }

    fun setCameraOffset(x: Float, y: Float) {
        glUniform2f(cameraOffsetLoc, x, y)
    }

    fun setTime(time: Float) {
        glUniform1f(timeLocation, time)
    }
}