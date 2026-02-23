package com.applauncher.util

import kotlin.test.Test
import kotlin.test.assertContains

class UpdateCheckerTest {

    @Test
    fun `buildUpdateScript contains silent msiexec install command`() {
        val script = UpdateChecker.buildUpdateScript(
            installerPath = "C:\\Temp\\AppLauncher-1.3.0.msi",
            exePath = "C:\\Users\\test\\AppData\\Local\\AppLauncher\\AppLauncher.exe"
        )
        assertContains(script, "/qn /i")
        assertContains(script, "C:\\Temp\\AppLauncher-1.3.0.msi")
        assertContains(script, "/norestart")
    }

    @Test
    fun `buildUpdateScript contains relaunch command with exe path`() {
        val script = UpdateChecker.buildUpdateScript(
            installerPath = "C:\\Temp\\installer.msi",
            exePath = "C:\\Users\\test\\AppLauncher.exe"
        )
        assertContains(script, "C:\\Users\\test\\AppLauncher.exe")
        assertContains(script, "Start-Process")
    }

    @Test
    fun `buildUpdateScript contains sleep and self-cleanup`() {
        val script = UpdateChecker.buildUpdateScript(
            installerPath = "C:\\Temp\\installer.msi",
            exePath = "C:\\exe\\app.exe"
        )
        assertContains(script, "Start-Sleep")
        assertContains(script, "Remove-Item")
    }
}
