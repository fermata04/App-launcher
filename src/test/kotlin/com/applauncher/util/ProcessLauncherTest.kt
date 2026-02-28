package com.applauncher.util

import com.applauncher.model.AppEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContains

class ProcessLauncherTest {

    @Test
    fun `buildAdminCommand uses powershell Start-Process with Verb RunAs`() {
        val entry = AppEntry(name = "Test", path = "C:\\Windows\\notepad.exe", runAsAdmin = true)
        val cmd = ProcessLauncher.buildAdminCommand(entry)
        assertEquals("powershell.exe", cmd[0])
        assertContains(cmd.joinToString(" "), "-Verb RunAs")
        assertContains(cmd.joinToString(" "), "C:\\Windows\\notepad.exe")
    }

    @Test
    fun `buildAdminCommand includes arguments when present`() {
        val entry = AppEntry(
            name = "Test",
            path = "C:\\app.exe",
            arguments = "--flag value",
            runAsAdmin = true
        )
        val cmd = ProcessLauncher.buildAdminCommand(entry)
        assertContains(cmd.joinToString(" "), "-ArgumentList")
        assertContains(cmd.joinToString(" "), "--flag value")
    }

    @Test
    fun `buildAdminCommand omits ArgumentList when arguments is blank`() {
        val entry = AppEntry(name = "Test", path = "C:\\app.exe", runAsAdmin = true)
        val cmd = ProcessLauncher.buildAdminCommand(entry)
        assert(!cmd.joinToString(" ").contains("-ArgumentList")) {
            "Should not contain -ArgumentList when arguments is blank"
        }
    }

    @Test
    fun `buildAdminCommand escapes single quotes in path`() {
        val entry = AppEntry(
            name = "Test",
            path = "C:\\My'App\\app.exe",
            runAsAdmin = true
        )
        val cmd = ProcessLauncher.buildAdminCommand(entry)
        assertContains(cmd.joinToString(" "), "My''App")
    }

    @Test
    fun `buildAdminCommand includes WorkingDirectory defaulting to exe parent dir`() {
        val entry = AppEntry(name = "Test", path = "D:\\Games\\App\\app.exe", runAsAdmin = true)
        val cmd = ProcessLauncher.buildAdminCommand(entry)
        assertContains(cmd.joinToString(" "), "-WorkingDirectory")
        assertContains(cmd.joinToString(" "), "D:\\Games\\App")
    }

    @Test
    fun `buildAdminCommand uses explicit workingDirectory when set`() {
        val entry = AppEntry(
            name = "Test",
            path = "D:\\Games\\App\\app.exe",
            workingDirectory = "D:\\Games\\App\\custom",
            runAsAdmin = true
        )
        val cmd = ProcessLauncher.buildAdminCommand(entry)
        assertContains(cmd.joinToString(" "), "-WorkingDirectory")
        assertContains(cmd.joinToString(" "), "D:\\Games\\App\\custom")
    }

    @Test
    fun `buildAdminCommand escapes single quotes in WorkingDirectory`() {
        val entry = AppEntry(
            name = "Test",
            path = "D:\\My'Games\\app.exe",
            runAsAdmin = true
        )
        val cmd = ProcessLauncher.buildAdminCommand(entry)
        assertContains(cmd.joinToString(" "), "-WorkingDirectory")
        assertContains(cmd.joinToString(" "), "My''Games")
    }
}
