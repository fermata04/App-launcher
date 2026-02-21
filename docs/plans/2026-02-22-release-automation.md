# Release Automation Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** タグ push をトリガーに MSI ビルド・SHA256SUMS.txt 生成・GitHub Release 作成を自動化し、isActive 未使用警告を解消する。

**Architecture:** GitHub Actions の `windows-latest` ランナーで `./gradlew packageMsi` を実行し、PowerShell で SHA256 を計算。`gh release create --generate-notes` でリリースを自動作成し、MSI と SHA256SUMS.txt をアップロードする。

**Tech Stack:** GitHub Actions, Gradle 8.5, JDK 17 (Temurin), PowerShell (SHA256 計算), gh CLI (GitHub Release 操作)

---

## Task 1: isActive パラメータ未使用警告を修正

**Files:**
- Modify: `src/main/kotlin/com/applauncher/ui/DropTargetArea.kt:23-24`
- Modify: `src/main/kotlin/com/applauncher/ui/MainScreen.kt:158-160, 238-240`

**背景:**
`DropTargetArea` の `isActive: Boolean` パラメータは関数本体で一切使用されていない。
呼び出し側（`MainScreen.kt`）は `isDropTargetActive` を直接参照しているため、このパラメータは不要。

**Step 1: DropTargetArea からパラメータを削除**

`src/main/kotlin/com/applauncher/ui/DropTargetArea.kt` の 23〜24 行目を変更:

```kotlin
// 変更前
@Composable
fun DropTargetArea(
    isActive: Boolean,
    onActiveChange: (Boolean) -> Unit,

// 変更後
@Composable
fun DropTargetArea(
    onActiveChange: (Boolean) -> Unit,
```

**Step 2: MainScreen.kt の呼び出し箇所を更新（2箇所）**

`src/main/kotlin/com/applauncher/ui/MainScreen.kt` の2箇所から `isActive = isDropTargetActive,` を削除:

```kotlin
// 変更前（158〜161行目付近）
DropTargetArea(
    isActive = isDropTargetActive,
    onActiveChange = { isDropTargetActive = it },
    modifier = Modifier.fillMaxSize()

// 変更後
DropTargetArea(
    onActiveChange = { isDropTargetActive = it },
    modifier = Modifier.fillMaxSize()
```

```kotlin
// 変更前（238〜241行目付近）
DropTargetArea(
    isActive = isDropTargetActive,
    onActiveChange = { isDropTargetActive = it },
    modifier = Modifier

// 変更後
DropTargetArea(
    onActiveChange = { isDropTargetActive = it },
    modifier = Modifier
```

**Step 3: コンパイル確認**

```bash
./gradlew compileKotlin
```

Expected: `BUILD SUCCESSFUL`（警告なし）

**Step 4: Commit**

```bash
git add src/main/kotlin/com/applauncher/ui/DropTargetArea.kt
git add src/main/kotlin/com/applauncher/ui/MainScreen.kt
git commit -m "Remove unused isActive parameter from DropTargetArea"
```

---

## Task 2: GitHub Actions ワークフローファイルを作成

**Files:**
- Create: `.github/workflows/release.yml`

**Step 1: ディレクトリ作成**

```bash
mkdir -p .github/workflows
```

**Step 2: ワークフローファイルを作成**

`.github/workflows/release.yml` を以下の内容で作成:

```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

permissions:
  contents: write

jobs:
  release:
    runs-on: windows-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build MSI
        shell: bash
        run: ./gradlew packageMsi

      - name: Find MSI and generate SHA256SUMS.txt
        id: sha256
        shell: pwsh
        run: |
          $msi = Get-ChildItem -Path "build/compose/binaries/main/msi" -Filter "*.msi" | Select-Object -First 1
          if (-not $msi) { throw "MSI file not found" }
          $hash = (Get-FileHash -Algorithm SHA256 -Path $msi.FullName).Hash.ToLower()
          "$hash  $($msi.Name)" | Out-File -FilePath "SHA256SUMS.txt" -Encoding utf8NoBOM
          echo "msi_path=$($msi.FullName)" >> $env:GITHUB_OUTPUT
          echo "msi_name=$($msi.Name)" >> $env:GITHUB_OUTPUT
          echo "MSI: $($msi.Name)"
          echo "SHA256: $hash"

      - name: Create GitHub Release
        shell: bash
        run: |
          gh release create "${{ github.ref_name }}" \
            --generate-notes \
            "${{ steps.sha256.outputs.msi_path }}" \
            "SHA256SUMS.txt"
        env:
          GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

**Step 3: ワークフローの内容を確認**

```bash
cat .github/workflows/release.yml
```

Expected: ファイルが正しく作成されている

**Step 4: Commit**

```bash
git add .github/workflows/release.yml
git commit -m "Add GitHub Actions release workflow for automated MSI build and SHA256SUMS.txt"
```

---

## Task 3: 動作確認

**Step 1: テスト用タグを push してワークフローをトリガー**

```bash
git tag v1.1.1-test
git push origin v1.1.1-test
```

**Step 2: GitHub Actions の実行を確認**

```bash
gh run list --workflow=release.yml
```

Expected: `v1.1.1-test` タグに対応する実行が表示される

**Step 3: ワークフローのログを確認**

```bash
gh run view --log
```

Expected: 全ステップが成功（✓）

**Step 4: リリースアセットを確認**

```bash
gh release view v1.1.1-test --json assets --jq '.assets[].name'
```

Expected:
```
AppLauncher-1.1.1.msi
SHA256SUMS.txt
```

**Step 5: テスト用リリース・タグを削除**

```bash
gh release delete v1.1.1-test --yes
git push origin --delete v1.1.1-test
git tag -d v1.1.1-test
```

---

## 補足: 次回リリース時のフロー

```bash
# 1. build.gradle.kts の version を更新してコミット
# 2. タグを打つだけで完了
git tag v1.2.0
git push origin v1.2.0
# → GitHub Actions が自動でビルド・リリース・SHA256SUMS.txt を作成
```
