# Silent In-App Update Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the manual MSI installer flow with a silent background install that auto-restarts the app.

**Architecture:** Write a temp PowerShell script to disk, launch it hidden (`-WindowStyle Hidden`), then exit the app. The script runs `msiexec /qn` silently, waits for completion, then re-launches the app from its original path.

**Tech Stack:** Kotlin, Compose Desktop (JVM), Windows MSI / msiexec, PowerShell

---

### Task 1: Add test dependency to build.gradle.kts

**Files:**
- Modify: `build.gradle.kts`

**Step 1: Add testImplementation to dependencies block**

In `build.gradle.kts`, add inside the `dependencies { }` block (after the existing lines):

```kotlin
testImplementation(kotlin("test"))
```

**Step 2: Verify Gradle sync succeeds**

```bash
./gradlew testClasses
```

Expected: `BUILD SUCCESSFUL`

**Step 3: Commit**

```bash
git add build.gradle.kts
git commit -m "build: add kotlin-test dependency"
```

---

### Task 2: Write failing tests for `buildUpdateScript()`

**Files:**
- Create: `src/test/kotlin/com/applauncher/util/UpdateCheckerTest.kt`

**Step 1: Create the test file**

```kotlin
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
```

**Step 2: Run tests to verify they fail**

```bash
./gradlew test
```

Expected: FAIL — `Unresolved reference: buildUpdateScript`

---

### Task 3: Implement `buildUpdateScript()` and `silentInstallAndRestart()` in UpdateChecker.kt

**Files:**
- Modify: `src/main/kotlin/com/applauncher/util/UpdateChecker.kt`

**Step 1: Replace `launchInstaller()` with two new functions**

Remove the existing `launchInstaller()` function (lines ~229–239):

```kotlin
fun launchInstaller(installerFile: File): Boolean {
    return try {
        ProcessBuilder("cmd", "/c", "start", "", installerFile.absolutePath)
            .directory(installerFile.parentFile)
            .start()
        true
    } catch (e: Exception) {
        AppLogger.error("Failed to launch installer: ${installerFile.absolutePath}", e)
        false
    }
}
```

Replace with these two functions:

```kotlin
fun silentInstallAndRestart(installerFile: File): Boolean {
    return try {
        val exePath = ProcessHandle.current().info().command().orElse(null)
            ?: run {
                AppLogger.warn("Could not determine current exe path for restart")
                return false
            }

        val scriptContent = buildUpdateScript(installerFile.absolutePath, exePath)
        val scriptFile = File(installerFile.parentFile, "update.ps1")
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
        false
    }
}

internal fun buildUpdateScript(installerPath: String, exePath: String): String {
    // Escape backticks and double-quotes for PowerShell string interpolation
    val safeInstaller = installerPath.replace("`", "``")
    val safeExe = exePath.replace("`", "``")
    return """
Start-Sleep -Seconds 2
`$p = Start-Process msiexec -ArgumentList "/qn /i `"$safeInstaller`" /norestart" -Wait -PassThru
if (`$p.ExitCode -eq 0) {
    Start-Process -FilePath "$safeExe"
}
Remove-Item `$MyInvocation.MyCommand.Path -Force -ErrorAction SilentlyContinue
    """.trimIndent()
}
```

**Step 2: Run tests to verify they pass**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`, all 3 tests PASS

**Step 3: Commit**

```bash
git add src/main/kotlin/com/applauncher/util/UpdateChecker.kt \
        src/test/kotlin/com/applauncher/util/UpdateCheckerTest.kt
git commit -m "feat: add silentInstallAndRestart with PowerShell bootstrap"
```

---

### Task 4: Update UpdateDialog.kt — labels and description text

**Files:**
- Modify: `src/main/kotlin/com/applauncher/ui/UpdateDialog.kt`

**Step 1: Update the `ReadyToInstall` state text**

Find this block (around line 117–127):

```kotlin
is UpdateState.ReadyToInstall -> {
    Text(
        text = "ダウンロードが完了しました。インストーラーを起動してアプリを更新します。",
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        text = "インストーラー起動後、アプリは自動的に終了します。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
}
```

Replace with:

```kotlin
is UpdateState.ReadyToInstall -> {
    Text(
        text = "ダウンロードが完了しました。バックグラウンドでサイレントインストールを実行します。",
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        text = "インストール完了後、アプリが自動的に再起動します。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
}
```

**Step 2: Update the confirm button label**

Find this block (around line 161–165):

```kotlin
is UpdateState.ReadyToInstall -> {
    Button(onClick = onInstallAndClose) {
        Text("インストールして終了")
    }
}
```

Replace with:

```kotlin
is UpdateState.ReadyToInstall -> {
    Button(onClick = onInstallAndClose) {
        Text("インストール")
    }
}
```

**Step 3: Commit**

```bash
git add src/main/kotlin/com/applauncher/ui/UpdateDialog.kt
git commit -m "feat: update UpdateDialog labels for silent install UX"
```

---

### Task 5: Update MainScreen.kt — use silentInstallAndRestart

**Files:**
- Modify: `src/main/kotlin/com/applauncher/ui/MainScreen.kt`

**Step 1: Replace launchInstaller call**

Find this block (around line 478–487):

```kotlin
onInstallAndClose = {
    val current = updateState
    if (current is UpdateState.ReadyToInstall) {
        if (UpdateChecker.launchInstaller(current.installerFile)) {
            onExitApplication()
        } else {
            snackbarMessage = "インストーラーの起動に失敗しました"
        }
    }
}
```

Replace with:

```kotlin
onInstallAndClose = {
    val current = updateState
    if (current is UpdateState.ReadyToInstall) {
        if (UpdateChecker.silentInstallAndRestart(current.installerFile)) {
            onExitApplication()
        } else {
            snackbarMessage = "アップデートの起動に失敗しました"
        }
    }
}
```

**Step 2: Run tests to confirm no regressions**

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`

**Step 3: Commit**

```bash
git add src/main/kotlin/com/applauncher/ui/MainScreen.kt
git commit -m "feat: wire silentInstallAndRestart in MainScreen"
```

---

### Task 6: Manual smoke test

Start the app in dev mode and verify the flow end-to-end:

1. Temporarily downgrade `version` in `build.gradle.kts` to `1.2.0`
2. Run: `./gradlew run`
3. The update notification badge should appear
4. Open the update dialog → click "ダウンロード"
5. After download completes, click "インストール"
6. App should close silently
7. After a few seconds, the app should re-open on its own at v1.3.0

Restore `version = "1.3.0"` after testing.
