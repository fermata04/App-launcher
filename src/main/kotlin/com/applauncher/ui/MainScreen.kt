package com.applauncher.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.applauncher.model.AppEntry
import com.applauncher.model.AppLauncherState
import com.applauncher.model.SortMode
import com.applauncher.util.ProcessLauncher

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(state: AppLauncherState) {
    val apps by state.apps.collectAsState()
    val displayApps by state.displayApps.collectAsState()
    val dragState by state.dragState.collectAsState()
    val sortMode by state.sortMode.collectAsState()
    val selectedTag by state.selectedTag.collectAsState()
    val allTags by state.allTags.collectAsState()

    var editingApp by remember { mutableStateOf<AppEntry?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isDropTargetActive by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // ドラッグオフセットの追跡（IDベース）
    var dragOffsets by remember { mutableStateOf(mapOf<String, Float>()) }

    val isDragEnabled = sortMode == SortMode.MANUAL && selectedTag == null

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
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
                    isActive = isDropTargetActive,
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

                    // ドロップエリア（上部）
                    DropTargetArea(
                        isActive = isDropTargetActive,
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

                    // アプリリスト
                    LazyColumn(
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
                            // IDベースでドラッグ状態を判定
                            val isDragging = dragState?.draggedId == app.id
                            val currentDragIndex = dragState?.currentIndex ?: -1
                            val isDropTarget = !isDragging &&
                                dragState != null &&
                                index == currentDragIndex

                            AppListItem(
                                entry = app,
                                index = index,
                                isDragging = isDragging,
                                isDropTarget = isDropTarget,
                                dragOffset = dragOffsets[app.id] ?: 0f,
                                isDragEnabled = isDragEnabled,
                                onLaunch = {
                                    if (ProcessLauncher.launch(app)) {
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
                                    // IDベースでドラッグ開始
                                    state.startDrag(app.id)
                                    dragOffsets = mapOf(app.id to 0f)
                                },
                                onDrag = { delta ->
                                    val currentOffset = (dragOffsets[app.id] ?: 0f) + delta
                                    dragOffsets = dragOffsets + (app.id to currentOffset)

                                    // 現在の実際のインデックスを取得
                                    val currentAppIndex = state.getAppIndex(app.id)
                                    if (currentAppIndex >= 0) {
                                        // 現在のドラッグ位置からターゲットインデックスを計算
                                        val itemHeight = 72f // おおよそのアイテム高さ
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
