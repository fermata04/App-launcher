# Compose 1.5.11 → 1.7.1 アップグレード実装プラン

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Compose を 1.5.11 から 1.7.1 にアップグレードし、非推奨 API（Divider、animateItemPlacement、LinearProgressIndicator）をすべて置き換えてビルドとテストを通す。

**Architecture:** バージョンバンプ → 非推奨 API 修正 → ビルド確認 の順に進める。各変更は独立しているため、ファイル単位でコミット可能。1.7.x で既に `HorizontalDivider()` に変更済みのファイル（UpdateDialog.kt, MainScreen.kt の Divider 箇所）は触れない。

**Tech Stack:** Kotlin 1.9.21, Jetbrains Compose Desktop 1.7.1, Material3, Gradle (Kotlin DSL)

---

## 前提確認

- ブランチ: `main`
- 1.7.1 の Gradle キャッシュが既にローカルに存在（ネットワーク不要）
  - `/c/Users/kaito/.gradle/caches/modules-2/files-2.1/org.jetbrains.compose/compose-gradle-plugin/1.7.1/`
- 現セッションで既に変更済み（触らない）:
  - `UpdateDialog.kt` line 76: `HorizontalDivider()` ✓
  - `MainScreen.kt` line 294: `HorizontalDivider()` ✓
  - `UpdateChecker.kt`: `downloadUpdate(asset, release)` シグネチャ変更 ✓

---

## Task 1: build.gradle.kts のバージョンバンプ

**Files:**
- Modify: `build.gradle.kts:5`

**Step 1: バージョンを変更**

```kotlin
// Before:
id("org.jetbrains.compose") version "1.5.11"

// After:
id("org.jetbrains.compose") version "1.7.1"
```

**Step 2: ビルドが通ることを確認（API 修正前なので失敗する）**

```bash
./gradlew compileKotlin 2>&1 | grep -E "error:|warning:|Unresolved"
```

Expected: `Unresolved reference: HorizontalDivider` 等のエラーが出る（この時点では正常）。
もし全くエラーが出ない場合は 1.7.x の deprecated API が実はそのまま動いている → Task 3〜5 は念のため実施する。

---

## Task 2: AppListItem.kt — Divider() → HorizontalDivider()

**Files:**
- Modify: `src/main/kotlin/com/applauncher/ui/AppListItem.kt:277,400,481`

3 箇所すべて同じ変更（DropdownMenu 内のセパレータ）。

**Step 1: 3 箇所を置き換え**

| 場所 | 変更前 | 変更後 |
|------|--------|--------|
| Line 277 (AppListItem の DropdownMenu) | `Divider()` | `HorizontalDivider()` |
| Line 400 (AppGridItem の DropdownMenu) | `Divider()` | `HorizontalDivider()` |
| Line 481 (RecentAppItem の DropdownMenu) | `Divider()` | `HorizontalDivider()` |

インポートは `import androidx.compose.material3.*` のワイルドカードで既に解決される。個別 import 追加は不要。

**Step 2: コンパイル確認**

```bash
./gradlew compileKotlin 2>&1 | grep "error:" | head -20
```

Expected: `AppListItem.kt` のエラーが消えている。

---

## Task 3: MainScreen.kt — animateItemPlacement() → animateItem()

**Files:**
- Modify: `src/main/kotlin/com/applauncher/ui/MainScreen.kt:385-391`

Compose 1.7.x で `animateItemPlacement()` が `animateItem()` に改名された。

**Step 1: 変更箇所を確認**

現在のコード（MainScreen.kt line 385-391）:
```kotlin
modifier = Modifier.animateItemPlacement(
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
)
```

**Step 2: 置き換え**

```kotlin
modifier = Modifier.animateItem(
    fadeInSpec = null,
    fadeOutSpec = null,
    placementSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
)
```

`animateItem` は引き続き `@ExperimentalFoundationApi` のため `@OptIn` が必要だが、関数の先頭に既に `@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)` がある。追加は不要。

`fadeInSpec = null, fadeOutSpec = null` はフェードアニメーションなしを意味し、1.5.x の動作と同等。

**Step 3: コンパイル確認**

```bash
./gradlew compileKotlin 2>&1 | grep "error:" | head -20
```

Expected: `MainScreen.kt` の `animateItemPlacement` エラーが消えている。

---

## Task 4: UpdateDialog.kt — LinearProgressIndicator(progress: Float → () -> Float)

**Files:**
- Modify: `src/main/kotlin/com/applauncher/ui/UpdateDialog.kt:99`

Compose 1.7.x で `LinearProgressIndicator(progress: Float)` が deprecated → `progress: () -> Float` に変更。

**Step 1: 変更箇所を確認**

現在のコード（UpdateDialog.kt line 98-102）:
```kotlin
LinearProgressIndicator(
    progress = updateState.progress,
    modifier = Modifier.fillMaxWidth()
)
```

Line 109 の不定形 `LinearProgressIndicator(modifier = Modifier.fillMaxWidth())` は引数なしのため変更不要。

**Step 2: ラムダに変更**

```kotlin
LinearProgressIndicator(
    progress = { updateState.progress },
    modifier = Modifier.fillMaxWidth()
)
```

`updateState` は `is UpdateState.Downloading` のスコープ内で使われているため、スマートキャストは引き続き機能する。

**Step 3: コンパイル確認**

```bash
./gradlew compileKotlin 2>&1 | grep "error:" | head -20
```

Expected: エラーなし（全修正完了後）。

---

## Task 5: フルビルドとテスト

**Step 1: テストを実行**

```bash
./gradlew test 2>&1 | tail -30
```

Expected: `BUILD SUCCESSFUL` で UpdateCheckerTest の 5 テストがすべて PASS。

**Step 2: warning 確認（任意）**

```bash
./gradlew build 2>&1 | grep -i "deprecat" | head -20
```

Expected: 新しい deprecation warning が出ないこと。出た場合はどの API か記録する（この PR スコープ外であれば HANDOVER に追記）。

---

## Task 6: コミット

**Step 1: 変更ファイルを確認**

```bash
git diff --stat
```

Expected:
```
build.gradle.kts          |  2 +-
src/.../AppListItem.kt    |  6 +++---
src/.../MainScreen.kt     |  8 ++++----
src/.../UpdateDialog.kt   |  2 +-
src/.../UpdateChecker.kt  | (前セッションの変更)
4 files changed, ...
```

**Step 2: コミット**

```bash
git add build.gradle.kts \
        src/main/kotlin/com/applauncher/ui/AppListItem.kt \
        src/main/kotlin/com/applauncher/ui/MainScreen.kt \
        src/main/kotlin/com/applauncher/ui/UpdateDialog.kt \
        src/main/kotlin/com/applauncher/util/UpdateChecker.kt

git commit -m "chore: upgrade Compose to 1.7.1, replace deprecated APIs

- Bump org.jetbrains.compose from 1.5.11 to 1.7.1
- Replace Divider() with HorizontalDivider() (AppListItem x3, UpdateDialog, MainScreen)
- Replace animateItemPlacement() with animateItem() (MainScreen)
- Replace LinearProgressIndicator(progress: Float) with lambda form (UpdateDialog)
- Fix downloadUpdate() double-fetch: pass GitHubRelease instead of re-fetching"
```

---

## スモークテストチェックリスト（手動）

ビルド後、実際に起動して以下を確認する:

- [ ] アプリが起動する
- [ ] リスト表示 → MoreVert ボタン → DropdownMenu にセパレータが表示される
- [ ] グリッド表示 → 右クリック → DropdownMenu にセパレータが表示される
- [ ] リスト表示でアプリをドラッグ＆ドロップ → アニメーション（animateItem）が機能する
- [ ] アップデートダイアログを開く（更新あり状態で） → プログレスバーが表示される

---

## ロールバック手順

問題が発生した場合:

```bash
# build.gradle.kts を 1.5.11 に戻す
git checkout build.gradle.kts

# UI ファイルを元に戻す
git checkout src/main/kotlin/com/applauncher/ui/
```
