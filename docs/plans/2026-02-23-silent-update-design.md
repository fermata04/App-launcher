# Silent In-App Update Design

**Date:** 2026-02-23
**Branch:** feature/silent-update (planned)

## Problem

Currently, updating the app requires the user to manually step through the MSI installer GUI after clicking "インストールして終了". The goal is to make the entire update process complete silently within the app.

## Requirements

- Silent install: no installer GUI shown to the user
- Auto-restart: after install completes, the new version launches automatically
- No changes to release pipeline: MSI stays as the distribution format

## Solution: PowerShell Bootstrap Script

### Flow

```
User clicks "インストール"
  → App writes a temp PowerShell script to %TEMP%\AppLauncher-update\update.ps1
  → App launches: powershell.exe -ExecutionPolicy Bypass -WindowStyle Hidden -File update.ps1
  → App exits (releases file locks on its own binaries)
  → PS: waits 2 seconds for process to fully exit
  → PS: runs msiexec /qn /i "installer.msi" /norestart (silent install)
  → PS: if exit code == 0, re-launches app from same exe path
  → PS: deletes itself
```

### PowerShell Script Template

```powershell
Start-Sleep -Seconds 2
$p = Start-Process msiexec -ArgumentList "/qn /i `"<msiPath>`" /norestart" -Wait -PassThru
if ($p.ExitCode -eq 0) {
    Start-Process -FilePath "<exePath>"
}
Remove-Item $MyInvocation.MyCommand.Path -Force -ErrorAction SilentlyContinue
```

### Re-launch Path

The current exe path is obtained via `ProcessHandle.current().info().command().orElse(null)`.
Since MSI upgrades overwrite the existing per-user installation in-place, the same path is valid for the new version.

## Files to Change

| File | Change |
|------|--------|
| `UpdateChecker.kt` | Replace `launchInstaller()` with `silentInstallAndRestart()`: get exe path, write PS script, launch with `-WindowStyle Hidden` |
| `UpdateDialog.kt` | Button label: "インストールして終了" → "インストール"; update description text |
| `MainScreen.kt` | No change to call site (`onInstallAndClose` callback stays the same) |

## Rejected Alternatives

| Approach | Reason rejected |
|----------|----------------|
| Batch script (.bat) | Console window briefly visible even when minimized |
| VBScript (.vbs) | Deprecated on Windows 11; future risk |
