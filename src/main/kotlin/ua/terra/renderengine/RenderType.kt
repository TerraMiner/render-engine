package ua.terra.renderengine

enum class RenderType(val value: Float) {
    TEXTURE(0f),
    FILL_RECT(1f),
    HOLL_RECT(2f),
    LINE(3f),
    TEXT(4f);
}