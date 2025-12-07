package ua.terra.renderengine.texture.manager

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import java.nio.ByteBuffer

class RawTexture(
    val id: Int,
    val width: Int,
    val height: Int,
) {
    constructor(width: Int, height: Int, data: ByteBuffer) : this(
        GL11.glGenTextures(),
        width,
        height
    ) {
        TextureManager.textureAmount++

        bind()

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGBA,
            width,
            height,
            0,
            GL11.GL_RGBA,
            GL11.GL_UNSIGNED_BYTE,
            data
        )

        unbind()
    }

    /** Binds this texture to the current OpenGL context */
    fun bind() = TextureManager.bindTex(id)

    /** Unbinds this texture from the current OpenGL context */
    fun unbind() = TextureManager.unbindTex()
}