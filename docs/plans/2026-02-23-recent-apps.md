# Recently Used Apps Section Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a "最近使ったアプリ" horizontal icon row above the app list/grid, showing the 5 most recently launched apps, hidden when filtering or searching.

**Architecture:** `recentApps: StateFlow<List<AppEntry>>` derived in `AppLauncherState.updateDerived()` from all apps (unfiltered, sorted by `lastLaunchedAt` desc, top 5); `RecentAppItem` composable added to `AppListItem.kt`; `MainScreen` shows the section with `AnimatedVisibility` when not filtering.

**Tech Stack:** Kotlin 1.9.21, Compose Desktop 1.5.11, Material3

> **Note:** No test framework configured. Verification via `./gradlew compileKotlin` and `./gradlew run`.

---

### Task 0: ブランチを作成する

**Files:** なし（git 操作のみ）

**Step 1: feature ブランチを作成して切り替える**

```bash
cd /c/Users/kaito/AppLauncher/app-launcher
git checkout -b feature/recent-apps
```

Expected: `Switched to a new branch 'feature/recent-apps'`

---

### Task 1: `recentApps` StateFlow を AppLauncherState に追加

**Files:**
- Modify: `src/main/kotlin/com/applauncher/model/AppLauncherState.kt`

**Step 1: `_recentApps` フィールドを追加**

`_displayApps` フィールドの直後（現在 line 46-47 付近）に追加:

```kotlin
// Derived: top 5 most recently launched apps (unfiltered)
private val _recentApps = MutableStateFlow<List<AppEntry>>(emptyList())
val recentApps: StateFlow<List<AppEntry>> = _recentApps.asStateFlow()
```

**Step 2: `updateDerived()` に recentApps の計算を追加**

`updateDerived()` メソッド内、`_displayApps.value = ...` の後に追加:

```kotlin
_recentApps.value = _apps.value
    .filter { it.lastLaunchedAt != null }
    .sortedByDescending { it.lastLaunchedAt }
    .take(5)
```

**Step 3: コンパイル確認**

```bash
cd /c/Users/kaito/AppLauncher/app-launcher && ./gradlew compileKotlin
```

Expected: `BUILD SUCCESSFUL`

**Step 4: コミット**

```bash
cd /c/Users/kaito/AppLauncher/app-launcher
git add src/main/kotlin/com/applauncher/model/AppLauncherState.kt
git commit -m "$(cat <<'EOF'
feat: add recentApps StateFlow to AppLauncherState

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: `RecentAppItem` コンポーザブルを追加

**Files:**
- Modify: `src/main/kotlin/com/applauncher/ui/AppListItem.kt`

**Step 1: ファイルを Read ツールで読んで既存 import を確認する**

`AppGridItem` に使われている import（`ExperimentalComposeUiApi`, `onPointerEvent`, `PointerEventType`, `TextAlign` 等）がすでに存在することを確認すること。

**Step 2: `AppGridItem` の閉じ括弧の後に `RecentAppItem` を追加**

ファイル末尾に追加:

```kotlin
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun RecentAppItem(
    entry: AppEntry,
    onLaunch: () -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }

    val overlayAlpha by animateFloatAsState(
        targetValue = if (isHovered) 0.6f else 0f,
        animationSpec = tween(150)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .onPointerEvent(PointerEventType.Enter) { isHovered = true }
            .onPointerEvent(PointerEventType.Exit) { isHovered = false }
            .combinedClickable(
                onClick = onLaunch,
                onLongClick = { showContextMenu = true }
            )
    ) {
        AppIcon(
            path = entry.path,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = overlayAlpha)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = entry.name,
                color = Color.White.copy(alpha = overlayAlpha),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 4.dp)
            )
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("起動") },
                onClick = { showContextMenu = false; onLaunch() },
                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("編集") },
                onClick = { showContextMenu = false; onEdit() },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
            Divider()
            DropdownMenuItem(
                text = { Text("削除", color = MaterialTheme.colorScheme.error) },
                onClick = { showContextMenu = false; onRemove() },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null,
                        tint = MaterialTheme.colorScheme.error)
                }
            )
        }
    }
}
```

**Step 3: コンパイル確認**

```bash
cd /c/Users/kaito/AppLauncher/app-launcher && ./gradlew compileKotlin
```

Expected: `BUILD SUCCESSFUL`

**Step 4: コミット**

```bash
cd /c/Users/kaito/AppLauncher/app-launcher
git add src/main/kotlin/com/applauncher/ui/AppListItem.kt
git commit -m "$(cat <<'EOF'
feat: add RecentAppItem composable with hover overlay

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: MainScreen に「最近使ったアプリ」セクションを追加

**Files:**
- Modify: `src/main/kotlin/com/applauncher/ui/MainScreen.kt`

**Step 1: ファイルを Read ツールで読む**

現在の構造を把握すること。特にタグフィルターバーの後、DropTargetArea の前の位置を確認。

**Step 2: import を追加**

既存 import ブロックに以下を追加（重複は追加しない）:

```kotlin
import androidx.compose.foundation.lazy.LazyRow
```

> `LazyRow` 以外（`AnimatedVisibility`, `fadeIn`, `fadeOut` 等）はすでに import 済みのはず。確認すること。

**Step 3: `recentApps` state を収集**

`MainScreen` 関数内、`val viewMode by state.viewMode.collectAsState()` の直後に追加:

```kotlin
val recentApps by state.recentApps.collectAsState()
```

**Step 4: セクションを挿入**

タグフィルターバー（`if (allTags.isNotEmpty()) { LazyRow { ... } }`）の直後、`// ドロップエリア（上部）` の直前に追加:

```kotlin
// 最近使ったアプリセクション
AnimatedVisibility(
    visible = recentApps.isNotEmpty() && selectedTag == null && searchQuery.isBlank(),
    enter = fadeIn() + expandVertically(),
    exit = fadeOut() + shrinkVertically()
) {
    Column {
        Text(
            text = "最近使ったアプリ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(recentApps, key = { it.id }) { app ->
                RecentAppItem(
                    entry = app,
                    onLaunch = {
                        if (ProcessLauncher.launch(app)) {
                            state.recordLaunch(app.id)
                            snackbarMessage = "${app.name} を起動しました"
                        } else {
                            snackbarMessage = "${app.name} の起動に失敗しました"
                        }
                    },
                    onRemove = {
                        state.removeApp(app.id)
                        snackbarMessage = "${app.name} を削除しました"
                    },
                    onEdit = { editingApp = app },
                    modifier = Modifier.size(72.dp)
                )
            }
        }
        Divider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
```

**Step 5: コンパイル確認**

```bash
cd /c/Users/kaito/AppLauncher/app-launcher && ./gradlew compileKotlin
```

Expected: `BUILD SUCCESSFUL`

**Step 6: 動作確認**

```bash
./gradlew run
```

確認チェックリスト:
- [ ] アプリを1つ以上起動すると「最近使ったアプリ」セクションが表示される
- [ ] 起動した順に並んでいる（最新が左端）
- [ ] タグフィルターを選択するとセクションが非表示になる（`AnimatedVisibility` でフェード）
- [ ] 検索バーに入力するとセクションが非表示になる
- [ ] フィルター・検索をクリアするとセクションが再表示される
- [ ] ホバーでアプリ名オーバーレイが表示される
- [ ] 長押し（右クリック）でコンテキストメニューが出る
- [ ] アプリ未起動の状態ではセクションが表示されない

**Step 7: コミット**

```bash
cd /c/Users/kaito/AppLauncher/app-launcher
git add src/main/kotlin/com/applauncher/ui/MainScreen.kt
git commit -m "$(cat <<'EOF'
feat: add recently used apps section to MainScreen

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```

---

## Done

全タスク完了後:

```bash
git log --oneline feature/recent-apps ^main
```

Expected:
```
xxxxxxx feat: add recently used apps section to MainScreen
xxxxxxx feat: add RecentAppItem composable with hover overlay
xxxxxxx feat: add recentApps StateFlow to AppLauncherState
xxxxxxx (branch creation)
```
