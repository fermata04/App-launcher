# Grid View Design

**Date:** 2026-02-22
**Branch:** main

## Overview

Add a grid view mode alongside the existing list view. Users can toggle between the two with a TopBar icon button. The selected view mode persists across restarts. In grid mode, each app is displayed as a large icon with a hover overlay that fades in the app name on a semi-transparent black background.

---

## Architecture

### New: `ViewMode` enum

Added to `src/main/kotlin/com/applauncher/model/AppLauncherState.kt` alongside `SortMode`:

```kotlin
enum class ViewMode { LIST, GRID }
```

### State persistence

`AppLauncherState` adds:
- `private val _viewMode = MutableStateFlow(ViewMode.LIST)`
- `val viewMode: StateFlow<ViewMode>`
- `fun toggleViewMode()`

View mode is persisted in a new `~/.applauncher/settings.json`:
```json
{ "viewMode": "LIST" }
```

A new `@Serializable data class AppSettings(val viewMode: ViewMode = ViewMode.LIST)` handles load/save. The existing `apps.json` is not modified.

---

## Components

### `AppGridItem` (new, in `AppListItem.kt`)

A self-contained composable for the grid cell:

```
Box(
  size = 120.dp,
  clip = RoundedCornerShape(8.dp),
  combinedClickable(onClick=onLaunch, onLongClick=showContextMenu)
  onPointerEvent(Enter) → isHovered = true
  onPointerEvent(Exit)  → isHovered = false
) {
  AppIcon(modifier = Modifier.fillMaxSize().padding(16.dp))  // ~88dp effective

  // Hover overlay - animated alpha
  val overlayAlpha by animateFloatAsState(if (isHovered) 0.6f else 0f)
  Box(
    fillMaxSize,
    background = Color.Black.copy(alpha = overlayAlpha),
    align = BottomCenter
  ) {
    Text(entry.name, color = White, maxLines = 1, overflow = Ellipsis)
  }
}
```

Parameters:
- `entry: AppEntry`
- `onLaunch: () -> Unit`
- `onRemove: () -> Unit`
- `onEdit: () -> Unit`
- `modifier: Modifier`

No drag parameters — grid view does not support manual reorder.

Context menu (same as AppListItem): Launch / Edit / Delete.

---

## MainScreen Changes

### TopBar action (added before sort button)

```kotlin
val viewMode by state.viewMode.collectAsState()

IconButton(onClick = { state.toggleViewMode() }) {
    Icon(
        imageVector = if (viewMode == ViewMode.GRID) Icons.Default.ViewList
                      else Icons.Default.GridView,
        contentDescription = "ビュー切替"
    )
}
```

### Content area

Replace the single `LazyColumn` block with a `when (viewMode)` branch:

```kotlin
when (viewMode) {
    ViewMode.LIST -> LazyColumn { /* existing AppListItem logic, unchanged */ }
    ViewMode.GRID -> LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(displayApps, key = { it.id }) { app ->
            AppGridItem(
                entry = app,
                onLaunch = { /* same as list */ },
                onRemove = { /* same as list */ },
                onEdit = { editingApp = app }
            )
        }
    }
}
```

Grid mode:
- `DropTargetArea` is hidden (D&D not supported in grid)
- Drag state logic is skipped entirely

---

## File Changes Summary

| File | Change |
|------|--------|
| `model/AppLauncherState.kt` | Add `ViewMode` enum, `_viewMode` state, `toggleViewMode()`, load/save settings.json |
| `ui/AppListItem.kt` | Add `AppGridItem` composable |
| `ui/MainScreen.kt` | Add viewMode TopBar button; add `when (viewMode)` branch in content area |

No new files needed (AppGridItem lives in AppListItem.kt alongside AppIcon).
