package com.applauncher.util

import com.applauncher.model.AppEntry
import java.io.File

object ProcessLauncher {
    
    fun launch(entry: AppEntry): Boolean {
        return try {
            val file = File(entry.path)
            if (!file.exists()) {
                println("File not found: ${entry.path}")
                return false
            }
            
            val processBuilder = ProcessBuilder()
            
            when (file.extension.lowercase()) {
                "exe", "bat", "cmd" -> {
                    val command = mutableListOf(entry.path)
                    if (entry.arguments.isNotBlank()) {
                        command.addAll(entry.arguments.split(" ").filter { it.isNotBlank() })
                    }
                    processBuilder.command(command)
                }
                "lnk" -> {
                    processBuilder.command("cmd", "/c", "start", "", entry.path)
                }
                "msc" -> {
                    processBuilder.command("mmc", entry.path)
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
            e.printStackTrace()
            false
        }
    }
}
