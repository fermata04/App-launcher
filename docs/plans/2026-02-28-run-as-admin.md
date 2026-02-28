# Run As Admin Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** アプリごとに「常に管理者権限で起動」を設定できるようにする。

**Architecture:** `AppEntry` に `runAsAdmin` フラグを追加し、`ProcessLauncher` で PowerShell `Start-Process -Verb RunAs` に委譲する。UI は `EditAppDialog` のみ変更（Switch トグル1行追加）。

**Tech Stack:** Kotlin, Compose for Desktop, kotlinx.serialization, PowerShell（追加ライブラリなし）

---

### Task 1: `AppEntry` に `runAsAdmin` フィールドを追加

**Files:**
- Modify: `src/main/kotlin/com/applauncher/model/AppEntry.kt`
- Test: `src/test/kotlin/com/applauncher/model/AppEntryTest.kt`（新規作成）

---

**Step 1: テストを書く（失敗させる）**

`src/test/kotlin/com/applauncher/model/AppEntryTest.kt` を新規作成:

```kotlin
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
```

**Step 2: テストを実行して失敗を確認**

```bash
./gradlew test --tests "com.applauncher.model.AppEntryTest" 2>&1 | tail -20
```

期待結果: `FAILED` （`runAsAdmin` フィールドが存在しないため）

---

**Step 3: `AppEntry.kt` に `runAsAdmin` を追加**

`src/main/kotlin/com/applauncher/model/AppEntry.kt` を以下に変更:

```kotlin
package com.applauncher.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class AppEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val path: String,
    val arguments: String = "",
    val workingDirectory: String = "",
    val tags: List<String> = emptyList(),
    val lastLaunchedAt: Long? = null,
    val runAsAdmin: Boolean = false
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
```

**Step 4: テストを実行して全件 PASS を確認**

```bash
./gradlew test --tests "com.applauncher.model.AppEntryTest" 2>&1 | tail -20
```

期待結果: `3 tests completed, 0 failed`

---

**Step 5: コミット**

```bash
git add src/main/kotlin/com/applauncher/model/AppEntry.kt \
        src/test/kotlin/com/applauncher/model/AppEntryTest.kt
git commit -m "feat: add runAsAdmin field to AppEntry"
```

---

### Task 2: `ProcessLauncher` に管理者起動ロジックを追加

**Files:**
- Modify: `src/main/kotlin/com/applauncher/util/ProcessLauncher.kt`
- Test: `src/test/kotlin/com/applauncher/util/ProcessLauncherTest.kt`（新規作成）

---

**Step 1: テストを書く（失敗させる）**

`src/test/kotlin/com/applauncher/util/ProcessLauncherTest.kt` を新規作成:

```kotlin
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
}
```

**Step 2: テストを実行して失敗を確認**

```bash
./gradlew test --tests "com.applauncher.util.ProcessLauncherTest" 2>&1 | tail -20
```

期待結果: `FAILED` （`buildAdminCommand` が存在しないため）

---

**Step 3: `ProcessLauncher.kt` に `buildAdminCommand` と管理者起動分岐を実装**

`src/main/kotlin/com/applauncher/util/ProcessLauncher.kt` を以下に置き換え:

```kotlin
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
```

**Step 4: テストを実行して全件 PASS を確認**

```bash
./gradlew test 2>&1 | tail -20
```

期待結果: 全テスト PASS（`UpdateCheckerTest` 5件 + `AppEntryTest` 3件 + `ProcessLauncherTest` 4件）

---

**Step 5: コミット**

```bash
git add src/main/kotlin/com/applauncher/util/ProcessLauncher.kt \
        src/test/kotlin/com/applauncher/util/ProcessLauncherTest.kt
git commit -m "feat: add admin launch logic to ProcessLauncher via PowerShell"
```

---

### Task 3: `EditAppDialog` に管理者として起動トグルを追加

**Files:**
- Modify: `src/main/kotlin/com/applauncher/ui/EditAppDialog.kt`

テストは手動のみ（Compose UI コンポーネントのため）。

---

**Step 1: `EditAppDialog.kt` に `runAsAdmin` の状態変数とトグル行を追加**

`EditAppDialog.kt` を変更する。主な変更点は2箇所:

**変更箇所 1:** `var tags` の直後に状態変数を追加（`EditAppDialog.kt:30` 付近）

変更前:
```kotlin
    var tags by remember(entry) { mutableStateOf(entry?.tags ?: emptyList()) }
    var newTagText by remember { mutableStateOf("") }
```

変更後:
```kotlin
    var tags by remember(entry) { mutableStateOf(entry?.tags ?: emptyList()) }
    var runAsAdmin by remember(entry) { mutableStateOf(entry?.runAsAdmin ?: false) }
    var newTagText by remember { mutableStateOf("") }
```

**変更箇所 2:** `// Tags section` の直前にトグル行を追加（`EditAppDialog.kt:112` 付近）

変更前:
```kotlin
                // Tags section
                Text(
                    text = "タグ",
                    style = MaterialTheme.typography.labelLarge
                )
```

変更後:
```kotlin
                // Admin toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "管理者として起動",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = runAsAdmin,
                        onCheckedChange = { runAsAdmin = it }
                    )
                }

                // Tags section
                Text(
                    text = "タグ",
                    style = MaterialTheme.typography.labelLarge
                )
```

**変更箇所 3:** 保存処理（`EditAppDialog.kt:198-210` 付近）に `runAsAdmin` を追加

変更前:
```kotlin
                        val updatedEntry = entry?.copy(
                            name = name,
                            path = path,
                            arguments = arguments,
                            workingDirectory = workingDirectory,
                            tags = tags
                        ) ?: AppEntry(
                            name = name,
                            path = path,
                            arguments = arguments,
                            workingDirectory = workingDirectory,
                            tags = tags
                        )
```

変更後:
```kotlin
                        val updatedEntry = entry?.copy(
                            name = name,
                            path = path,
                            arguments = arguments,
                            workingDirectory = workingDirectory,
                            tags = tags,
                            runAsAdmin = runAsAdmin
                        ) ?: AppEntry(
                            name = name,
                            path = path,
                            arguments = arguments,
                            workingDirectory = workingDirectory,
                            tags = tags,
                            runAsAdmin = runAsAdmin
                        )
```

---

**Step 2: ビルドを確認**

```bash
./gradlew compileKotlin 2>&1 | tail -20
```

期待結果: `BUILD SUCCESSFUL`

---

**Step 3: コミット**

```bash
git add src/main/kotlin/com/applauncher/ui/EditAppDialog.kt
git commit -m "feat: add run-as-admin toggle to EditAppDialog"
```

---

### Task 4: 手動スモークテスト

アプリを起動して以下を確認する:

```bash
./gradlew run
```

チェックリスト:
- [ ] 既存のアプリエントリが通常どおり起動する（既存動作に影響なし）
- [ ] アプリ編集ダイアログに「管理者として起動」スイッチが表示される
- [ ] スイッチ ON で保存 → そのアプリをクリック → UAC ダイアログが表示される
- [ ] UAC でキャンセルしてもアプリがクラッシュしない

---

### Task 5: バージョンアップ

**Files:**
- Modify: `build.gradle.kts:10`

`v1.4.0` → `v1.5.0` に更新:

```kotlin
version = "1.5.0"
```

```bash
git add build.gradle.kts
git commit -m "chore: bump version to 1.5.0"
```
