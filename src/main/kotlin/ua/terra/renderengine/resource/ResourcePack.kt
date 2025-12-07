package ua.terra.renderengine.resource

import java.io.File

data class ResourcePack(
    val id: String,
    val directory: File,
    val metadata: ResourcePackMetadata
)