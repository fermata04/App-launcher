package com.applauncher.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String = "",
    val body: String = "",
    @SerialName("html_url") val htmlUrl: String = "",
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubAsset> = emptyList()
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    val size: Long = 0,
    @SerialName("content_type") val contentType: String = ""
)
