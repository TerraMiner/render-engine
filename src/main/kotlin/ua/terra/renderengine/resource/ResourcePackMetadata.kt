package ua.terra.renderengine.resource

data class ResourcePackMetadata(
    val name: String,
    val description: String = "",
    val version: String = "1.0",
    val author: String = "",
    val website: String = ""
)