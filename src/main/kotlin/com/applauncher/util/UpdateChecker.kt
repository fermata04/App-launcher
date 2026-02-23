package com.applauncher.util

import com.applauncher.model.GitHubAsset
import com.applauncher.model.GitHubRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Properties
import java.util.concurrent.TimeUnit

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class Available(val release: GitHubRelease, val asset: GitHubAsset) : UpdateState()
    object UpToDate : UpdateState()
    data class Downloading(val progress: Float, val bytesRead: Long, val totalBytes: Long) : UpdateState()
    data class ReadyToInstall(val installerFile: File) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

object UpdateChecker {
    private const val RELEASES_URL =
        "https://api.github.com/repos/fermata04/App-launcher/releases/latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    fun getCurrentVersion(): String {
        return try {
            val props = Properties()
            val stream = UpdateChecker::class.java.getResourceAsStream("/version.properties")
            if (stream != null) {
                props.load(stream)
                stream.close()
                props.getProperty("version", "0.0.0")
            } else {
                "0.0.0"
            }
        } catch (e: Exception) {
            "0.0.0"
        }
    }

    suspend fun checkForUpdate(): GitHubRelease? = withContext(Dispatchers.IO) {
        _state.value = UpdateState.Checking
        try {
            val request = Request.Builder()
                .url(RELEASES_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .build()

            val response = client.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    _state.value = UpdateState.Error("サーバーエラー: ${resp.code}")
                    return@withContext null
                }

                val body = resp.body?.string() ?: run {
                    _state.value = UpdateState.Error("レスポンスが空です")
                    return@withContext null
                }

                val release = json.decodeFromString<GitHubRelease>(body)

                if (release.draft || release.prerelease) {
                    _state.value = UpdateState.UpToDate
                    return@withContext null
                }

                val latestVersion = release.tagName.removePrefix("v")
                val currentVersion = getCurrentVersion()

                if (isNewerVersion(latestVersion, currentVersion)) {
                    val asset = findInstallerAsset(release.assets)
                    if (asset != null) {
                        _state.value = UpdateState.Available(release, asset)
                        release
                    } else {
                        _state.value = UpdateState.Error("インストーラーが見つかりません")
                        null
                    }
                } else {
                    _state.value = UpdateState.UpToDate
                    null
                }
            }
        } catch (e: Exception) {
            _state.value = UpdateState.Error(
                "アップデート確認に失敗: ${e.localizedMessage ?: e.message ?: "不明なエラー"}"
            )
            null
        }
    }

    suspend fun downloadUpdate(asset: GitHubAsset): File? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(asset.browserDownloadUrl)
                .build()

            val response = client.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    _state.value = UpdateState.Error("ダウンロード失敗: ${resp.code}")
                    return@withContext null
                }

                val responseBody = resp.body ?: run {
                    _state.value = UpdateState.Error("ダウンロードレスポンスが空です")
                    return@withContext null
                }

                val totalBytes = responseBody.contentLength()
                val downloadDir = File(System.getProperty("java.io.tmpdir"), "AppLauncher-update")
                downloadDir.mkdirs()
                val outputFile = File(downloadDir, asset.name)

                responseBody.byteStream().use { inputStream ->
                    FileOutputStream(outputFile).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Long = 0
                        var read: Int

                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                            bytesRead += read
                            val progress = if (totalBytes > 0) {
                                bytesRead.toFloat() / totalBytes.toFloat()
                            } else {
                                -1f
                            }
                            _state.value = UpdateState.Downloading(
                                progress = progress,
                                bytesRead = bytesRead,
                                totalBytes = totalBytes
                            )
                        }
                    }
                }

                // SHA-256 ハッシュ検証
                val expectedHash = fetchExpectedHash(asset.name)
                if (expectedHash != null) {
                    if (!verifyFileHash(outputFile, expectedHash)) {
                        outputFile.delete()
                        _state.value = UpdateState.Error("ファイルの整合性検証に失敗しました")
                        return@withContext null
                    }
                }

                _state.value = UpdateState.ReadyToInstall(outputFile)
                outputFile
            }
        } catch (e: Exception) {
            _state.value = UpdateState.Error(
                "ダウンロードエラー: ${e.localizedMessage ?: e.message ?: "不明なエラー"}"
            )
            null
        }
    }

    private fun verifyFileHash(file: File, expectedHash: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
        return actualHash.equals(expectedHash, ignoreCase = true)
    }

    private fun fetchExpectedHash(assetName: String): String? {
        try {
            val request = Request.Builder()
                .url(RELEASES_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val releaseBody = response.use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.string() ?: return null
            }
            val release = json.decodeFromString<GitHubRelease>(releaseBody)
            val hashAsset = release.assets.firstOrNull {
                it.name.equals("SHA256SUMS.txt", ignoreCase = true)
            } ?: return null

            val hashRequest = Request.Builder()
                .url(hashAsset.browserDownloadUrl)
                .build()
            val hashResponse = client.newCall(hashRequest).execute()
            val hashContent = hashResponse.use { hashResp ->
                if (!hashResp.isSuccessful) return null
                hashResp.body?.string() ?: return null
            }
            // Format: "<hash>  <filename>" or "<hash> <filename>"
            return hashContent.lines()
                .map { it.trim() }
                .firstOrNull { line -> line.endsWith(assetName, ignoreCase = true) }
                ?.split("\\s+".toRegex())
                ?.firstOrNull()
        } catch (e: Exception) {
            AppLogger.warn("Failed to fetch hash for $assetName", e)
            return null
        }
    }

    /**
     * Starts a silent MSI installation via a PowerShell bootstrap script, then signals
     * success so the caller can exit the application.
     *
     * Returns `true` if the PowerShell process was successfully spawned — NOT that the
     * installation completed. The caller must exit the application immediately after this
     * returns `true`, so file locks are released before the installer runs.
     *
     * Returns `false` if the current exe path cannot be determined or if launching
     * PowerShell fails.
     */
    fun silentInstallAndRestart(installerFile: File): Boolean {
        val exePath = ProcessHandle.current().info().command().orElse(null)
            ?: run {
                AppLogger.warn("Could not determine current exe path for restart")
                return false
            }

        val scriptContent = buildUpdateScript(installerFile.absolutePath, exePath)
        val scriptFile = File(installerFile.parentFile, "update.ps1")

        return try {
            scriptFile.writeText(scriptContent)
            ProcessBuilder(
                "powershell.exe",
                "-ExecutionPolicy", "Bypass",
                "-WindowStyle", "Hidden",
                "-NonInteractive",
                "-File", scriptFile.absolutePath
            )
                .directory(installerFile.parentFile)
                .start()

            true
        } catch (e: Exception) {
            AppLogger.error("Failed to start silent update", e)
            runCatching { scriptFile.delete() }
            false
        }
    }

    internal fun buildUpdateScript(installerPath: String, exePath: String): String {
        // Escape backticks, dollar signs, and double-quotes for PowerShell string interpolation
        val safeInstaller = installerPath.replace("`", "``").replace("$", "`$").replace("\"", "`\"")
        val safeExe = exePath.replace("`", "``").replace("$", "`$").replace("\"", "`\"")
        return """
Start-Sleep -Seconds 2
${'$'}p = Start-Process msiexec -ArgumentList "/qn /i `"$safeInstaller`" /norestart" -Wait -PassThru
if (${'$'}p.ExitCode -eq 0) {
    Start-Process -FilePath "`"$safeExe`""
}
Remove-Item ${'$'}MyInvocation.MyCommand.Path -Force -ErrorAction SilentlyContinue
        """.trimIndent()
    }

    fun reset() {
        _state.value = UpdateState.Idle
    }

    internal fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun findInstallerAsset(assets: List<GitHubAsset>): GitHubAsset? {
        val msi = assets.firstOrNull {
            it.name.endsWith(".msi", ignoreCase = true)
        }
        if (msi != null) return msi

        return assets.firstOrNull {
            it.name.endsWith(".exe", ignoreCase = true)
        }
    }
}
