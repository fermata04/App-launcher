package com.applauncher.model

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppEntryTest {

    @Test
    fun `runAsAdmin defaults to false`() {
        val entry = AppEntry(name = "Test", path = "C:\\test.exe")
        assertFalse(entry.runAsAdmin)
    }

    @Test
    fun `runAsAdmin serializes and deserializes correctly`() {
        val entry = AppEntry(name = "Test", path = "C:\\test.exe", runAsAdmin = true)
        val json = Json.encodeToString(AppEntry.serializer(), entry)
        val decoded = Json.decodeFromString(AppEntry.serializer(), json)
        assertTrue(decoded.runAsAdmin)
    }

    @Test
    fun `existing JSON without runAsAdmin deserializes with false default`() {
        val json = """{"id":"abc","name":"Test","path":"C:\\test.exe","arguments":"","workingDirectory":"","tags":[]}"""
        val entry = Json.decodeFromString(AppEntry.serializer(), json)
        assertFalse(entry.runAsAdmin)
    }
}
