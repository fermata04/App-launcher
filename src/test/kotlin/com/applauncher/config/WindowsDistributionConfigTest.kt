package com.applauncher.config

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains

class WindowsDistributionConfigTest {

    @Test
    fun `windows distribution does not request a desktop shortcut`() {
        val buildScript = File("build.gradle.kts").readText()

        assertContains(buildScript, "shortcut = false")
    }
}
