package com.applauncher.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.applauncher.model.AppEntry
import com.applauncher.model.AppLauncherState
import com.applauncher.model.SortMode
import com.applauncher.model.ViewMode
import com.applauncher.util.ProcessLauncher
import com.applauncher.util.UpdateChecker
import com.applauncher.util.UpdateState
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(state: AppLauncherState, onExitApplication: () -> Unit = {}) {
    val apps by state.apps.collectAsState()
    val displayApps by state.displayApps.collectAsState()
    val dragState by state.dragState.collectAsState()
    val sortMode by state.sortMode.collectAsState()
    val selectedTag by state.selectedTag.collectAsState()
    val allTags by state.allTags.collectAsState()
    val searchQuery by state.searchQuery.collectAsState()
    val viewMode by state.viewMode.collectAsState()
    val recentApps by state.recentApps.collectAsState()

    var showSearchBar by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    var editingApp by remember { mutableStateOf<AppEntry?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isDropTargetActive by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val updateState by UpdateChecker.state.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // ドラッグオフセットの追跡（IDベース）
    var dragOffsets by remember { mutableStateOf(mapOf<String, Float>()) }

    val isDragEnabled = sortMode == SortMode.MANUAL && selectedTag == null && searchQuery.isBlank()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    // 起動時にアップデートを自動チェック
    LaunchedEffect(Unit) {
        val result = UpdateChecker.checkForUpdate()
        if (result != null) {
            showUpdateDialog = true
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("App Launcher") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    // View mode toggle button
                    IconButton(onClick = {
                        state.cancelDrag()
                        dragOffsets = emptyMap()
                        isDropTargetActive = false
                        state.toggleViewMode()
                    }) {
                        Icon(
                            imageVector = if (viewMode == ViewMode.GRID) Icons.Default.ViewList
                                          else Icons.Default.GridView,
                            contentDescription = if (viewMode == ViewMode.GRID) "リスト表示" else "グリッド表示"
                        )
                    }

                    // Search toggle button
                    IconButton(onClick = {
                        showSearchBar = !showSearchBar
                        if (!showSearchBar) {
                            state.setSearchQuery("")
                        }
                    }) {
                        Icon(
                            imageVector = if (showSearchBar) Icons.Default.SearchOff else Icons.Default.Search,
                            contentDescription = "検索"
                        )
                    }

                    // Sort toggle button
                    IconButton(onClick = { state.toggleSortMode() }) {
                        Icon(
                            imageVector = when (sortMode) {
                                SortMode.MANUAL -> Icons.Default.SwapVert
                                SortMode.NAME_ASC -> Icons.Default.ArrowUpward
                                SortMode.NAME_DESC -> Icons.Default.ArrowDownward
                            },
                            contentDescription = when (sortMode) {
                                SortMode.MANUAL -> "Sort: Manual"
                                SortMode.NAME_ASC -> "Sort: A-Z"
                                SortMode.NAME_DESC -> "Sort: Z-A"
                            }
                        )
                    }

                    // アップデート確認ボタン
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                UpdateChecker.checkForUpdate()
                                showUpdateDialog = true
                            }
                        }
                    ) {
                        if (updateState is UpdateState.Available) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = MaterialTheme.colorScheme.error)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdateAlt,
                                    contentDescription = "アップデート確認"
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.SystemUpdateAlt,
                                contentDescription = "アップデート確認"
                            )
                        }
                    }

                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add App"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (apps.isEmpty()) {
                // 空の状態 - ドロップターゲット
                DropTargetArea(
                    onActiveChange = { isDropTargetActive = it },
                    modifier = Modifier.fillMaxSize()
                ) {
                    EmptyStateContent(
                        onAddClick = { showAddDialog = true }
                    )
                }
            } else {
                // アプリリスト
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search bar
                    AnimatedVisibility(
                        visible = showSearchBar,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { state.setSearchQuery(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .focusRequester(searchFocusRequester),
                            placeholder = { Text("アプリ名で検索...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { state.setSearchQuery("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "クリア"
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        LaunchedEffect(Unit) {
                            searchFocusRequester.requestFocus()
                        }
                    }

                    // Tag filter bar
                    if (allTags.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedTag == null,
                                    onClick = { state.setTagFilter(null) },
                                    label = { Text("すべて") }
                                )
                            }
                            items(allTags) { tag ->
                                FilterChip(
                                    selected = selectedTag == tag,
                                    onClick = {
                                        state.setTagFilter(if (selectedTag == tag) null else tag)
                                    },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }

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

                    // ドロップエリア（上部）- リスト表示時のみ
                    if (viewMode == ViewMode.LIST) {
                        DropTargetArea(
                            onActiveChange = { isDropTargetActive = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isDropTargetActive) 80.dp else 0.dp)
                                .animateContentSize()
                        ) {
                            if (isDropTargetActive) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DropTarget.copy(alpha = 0.2f))
                                        .border(2.dp, DropTarget, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "ここにドロップしてアプリを追加",
                                        color = DropTarget
                                    )
                                }
                            }
                        }
                    }

                    // アプリリスト
                    when (viewMode) {
                        ViewMode.LIST -> LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            itemsIndexed(
                                items = displayApps,
                                key = { _, app -> app.id }
                            ) { index, app ->
                                val isDragging = dragState?.draggedId == app.id
                                val currentDragIndex = dragState?.currentIndex ?: -1
                                val isDropTarget = !isDragging &&
                                    dragState != null &&
                                    index == currentDragIndex

                                AppListItem(
                                    entry = app,
                                    isDragging = isDragging,
                                    isDropTarget = isDropTarget,
                                    dragOffset = dragOffsets[app.id] ?: 0f,
                                    isDragEnabled = isDragEnabled,
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
                                    onDragStart = {
                                        state.startDrag(app.id)
                                        dragOffsets = mapOf(app.id to 0f)
                                    },
                                    onDrag = { delta ->
                                        val currentOffset = (dragOffsets[app.id] ?: 0f) + delta
                                        dragOffsets = dragOffsets + (app.id to currentOffset)
                                        val currentAppIndex = state.getAppIndex(app.id)
                                        if (currentAppIndex >= 0) {
                                            val itemHeight = 72f
                                            val draggedItems = (currentOffset / itemHeight).toInt()
                                            val newIndex = (currentAppIndex + draggedItems).coerceIn(0, displayApps.size - 1)
                                            state.updateDragPosition(newIndex)
                                        }
                                    },
                                    onDragEnd = {
                                        state.endDrag()
                                        dragOffsets = emptyMap()
                                    },
                                    modifier = Modifier.animateItemPlacement(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    )
                                )
                            }
                        }

                        ViewMode.GRID -> LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 80.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            items(
                                items = displayApps,
                                key = { app -> app.id }
                            ) { app ->
                                AppGridItem(
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
                                    onEdit = { editingApp = app }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 編集ダイアログ
    if (editingApp != null) {
        EditAppDialog(
            entry = editingApp,
            allTags = allTags,
            onDismiss = { editingApp = null },
            onSave = { updatedApp ->
                state.updateApp(updatedApp)
                editingApp = null
                snackbarMessage = "${updatedApp.name} を更新しました"
            }
        )
    }

    // 追加ダイアログ
    if (showAddDialog) {
        EditAppDialog(
            entry = null,
            allTags = allTags,
            onDismiss = { showAddDialog = false },
            onSave = { newApp ->
                state.addApp(newApp)
                showAddDialog = false
                snackbarMessage = "${newApp.name} を追加しました"
            }
        )
    }

    // アップデートダイアログ
    if (showUpdateDialog && updateState !is UpdateState.Idle) {
        UpdateDialog(
            updateState = updateState,
            onDismiss = {
                showUpdateDialog = false
                if (updateState is UpdateState.UpToDate || updateState is UpdateState.Error) {
                    UpdateChecker.reset()
                }
            },
            onStartDownload = {
                val current = updateState
                if (current is UpdateState.Available) {
                    coroutineScope.launch {
                        UpdateChecker.downloadUpdate(current.asset)
                    }
                }
            },
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
        )
    }
}

@Composable
fun EmptyStateContent(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Apps,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "アプリが登録されていません",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "実行ファイル（.exe, .lnk など）を\nここにドラッグ＆ドロップしてください",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onAddClick) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("手動で追加")
        }
    }
}
