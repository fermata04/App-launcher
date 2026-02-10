package com.applauncher.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AppEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val path: String,
    val iconPath: String? = null,
    val arguments: String = "",
    val workingDirectory: String = ""
) {
    companion object {
        fun fromPath(path: String): AppEntry {
            val file = java.io.File(path)
            val name = file.nameWithoutExtension
            return AppEntry(
                name = name,
                path = path,
                workingDirectory = file.parent ?: ""
            )
        }
    }
}
