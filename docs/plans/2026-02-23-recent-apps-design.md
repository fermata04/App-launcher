# Recently Used Apps Section Design

**Date:** 2026-02-23
**Branch:** feature/recent-apps

## Overview

Add a "最近使ったアプリ" (Recently Used Apps) section at the top of the app list/grid. Shows the 5 most recently launched apps as a horizontal icon row. Hidden when tag filter or search is active.

---

## Architecture

### State: `recentApps` derived StateFlow

Added to `AppLauncherState`:

```kotlin
private val _recentApps = MutableStateFlow<List<AppEntry>>(emptyList())
val recentApps: StateFlow<List<AppEntry>> = _recentApps.asStateFlow()
```

Computed in `updateDerived()` from **all apps** (not filtered `displayApps`):

```kotlin
_recentApps.value = _apps.value
    .filter { it.lastLaunchedAt != null }
    .sortedByDescending { it.lastLaunchedAt }
    .take(5)
```

Key: derived from `_apps` (unfiltered), so independent of tag/search filters.

---

## UI Component: `RecentAppItem` (new, in `AppListItem.kt`)

A small icon cell, similar to `AppGridItem` but smaller:

- Size: `72.dp` square (`fillMaxWidth().aspectRatio(1f)` within the row)
- `AppIcon` fills the cell with padding
- Hover overlay: same fade animation as `AppGridItem` (`animateFloatAsState`, 150ms)
- `onClick` → launch, `onLongClick` → context menu (起動・編集・削除)
- No drag parameters

---

## MainScreen Changes

### New section above list/grid

Shown when: `recentApps.isNotEmpty() && selectedTag == null && searchQuery.isBlank()`

```
Text("最近使ったアプリ", labelMedium, secondary color)

LazyRow(
  horizontalArrangement = spacedBy(8.dp),
  contentPadding = PaddingValues(horizontal = 16.dp)
) {
  items(recentApps) { app ->
    RecentAppItem(
      entry = app,
      onLaunch = { ... },
      onRemove = { ... },
      onEdit = { ... },
      modifier = Modifier.size(72.dp)
    )
  }
}

Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
```

### Placement

Inserted after the tag filter bar and before the DropTargetArea, wrapped in `AnimatedVisibility` for smooth show/hide.

---

## File Changes Summary

| File | Change |
|------|--------|
| `model/AppLauncherState.kt` | Add `_recentApps` StateFlow; update `updateDerived()` |
| `ui/AppListItem.kt` | Add `RecentAppItem` composable |
| `ui/MainScreen.kt` | Collect `recentApps`; add section with `AnimatedVisibility` |
