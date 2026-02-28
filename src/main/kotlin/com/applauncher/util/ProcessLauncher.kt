package com.applauncher.util

import com.applauncher.model.AppEntry
import java.io.File

object ProcessLauncher {

    private fun parseArguments(args: String): List<String> {
        val result = mutableListOf<String>()
        val regex = Regex(""""([^"]*)"|(\S+)""")
        regex.findAll(args).forEach { match ->
            result.add(match.groupValues[1].ifEmpty { match.groupValues[2] })
        }
        return result
    }

    internal fun buildAdminCommand(entry: AppEntry): List<String> {
        val escapedPath = entry.path.replace("'", "''")
        val psCommand = buildString {
            append("Start-Process -FilePath '$escapedPath'")
            if (entry.arguments.isNotBlank()) {
                val escapedArgs = entry.arguments.replace("'", "''")
                append(" -ArgumentList '$escapedArgs'")
            }
            append(" -Verb RunAs")
        }
        return listOf("powershell.exe", "-NonInteractive", "-Command", psCommand)
    }

    fun launch(entry: AppEntry): Boolean {
        return try {
            val file = File(entry.path)
            if (!file.exists()) {
                println("File not found: ${entry.path}")
                return false
            }

            if (entry.runAsAdmin) {
                ProcessBuilder(buildAdminCommand(entry)).start()
                return true
            }

            val processBuilder = ProcessBuilder()

            when (file.extension.lowercase()) {
                "exe", "bat", "cmd" -> {
                    val command = mutableListOf(entry.path)
                    if (entry.arguments.isNotBlank()) {
                        command.addAll(parseArguments(entry.arguments))
                    }
                    processBuilder.command(command)
                }
                "lnk" -> {
                    processBuilder.command("cmd", "/c", "start", "", entry.path)
                }
                else -> {
                    processBuilder.command("cmd", "/c", "start", "", entry.path)
                }
            }

            if (entry.workingDirectory.isNotBlank()) {
                val workDir = File(entry.workingDirectory)
                if (workDir.exists() && workDir.isDirectory) {
                    processBuilder.directory(workDir)
                }
            }

            processBuilder.start()
            true
        } catch (e: Exception) {
            AppLogger.error("Failed to launch: ${entry.path}", e)
            false
        }
    }
}
