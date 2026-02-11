package com.applauncher.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

enum class SortMode {
    MANUAL, NAME_ASC, NAME_DESC
}

class AppLauncherState {
    private val configFile = File(System.getProperty("user.home"), ".applauncher/apps.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _dragState = MutableStateFlow<DragState?>(null)
    val dragState: StateFlow<DragState?> = _dragState.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.MANUAL)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag.asStateFlow()

    // Derived: all unique tags across all apps
    private val _allTags = MutableStateFlow<List<String>>(emptyList())
    val allTags: StateFlow<List<String>> = _allTags.asStateFlow()

    // Derived: filtered + sorted list for display
    private val _displayApps = MutableStateFlow<List<AppEntry>>(emptyList())
    val displayApps: StateFlow<List<AppEntry>> = _displayApps.asStateFlow()

    init {
        loadApps()
        updateDerived()
    }

    private fun updateDerived() {
        // Update allTags
        _allTags.value = _apps.value
            .flatMap { it.tags }
            .distinct()
            .sorted()

        // Update displayApps
        val filtered = _selectedTag.value?.let { tag ->
            _apps.value.filter { it.tags.contains(tag) }
        } ?: _apps.value

        _displayApps.value = when (_sortMode.value) {
            SortMode.MANUAL -> filtered
            SortMode.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
            SortMode.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
        }
    }

    private fun loadApps() {
        try {
            if (configFile.exists()) {
                val content = configFile.readText()
                _apps.value = json.decodeFromString<List<AppEntry>>(content)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveApps() {
        try {
            configFile.parentFile?.mkdirs()
            configFile.writeText(json.encodeToString(_apps.value))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addApp(entry: AppEntry) {
        if (_apps.value.none { it.path.equals(entry.path, ignoreCase = true) }) {
            _apps.value = _apps.value + entry
            saveApps()
            updateDerived()
        }
    }

    fun removeApp(id: String) {
        _apps.value = _apps.value.filter { it.id != id }
        saveApps()
        updateDerived()
    }

    fun updateApp(entry: AppEntry) {
        _apps.value = _apps.value.map { if (it.id == entry.id) entry else it }
        saveApps()
        updateDerived()
    }

    fun moveApp(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex < 0 || toIndex < 0) return
        if (fromIndex >= _apps.value.size || toIndex >= _apps.value.size) return

        val list = _apps.value.toMutableList()
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _apps.value = list
        saveApps()
        updateDerived()
    }

    fun startDrag(appId: String) {
        // Disable drag when sort/filter is active
        if (_sortMode.value != SortMode.MANUAL || _selectedTag.value != null) return

        val index = _apps.value.indexOfFirst { it.id == appId }
        if (index >= 0) {
            _dragState.value = DragState(
                draggedId = appId,
                draggedIndex = index,
                currentIndex = index
            )
        }
    }

    fun updateDragPosition(targetIndex: Int) {
        _dragState.value?.let { state ->
            val clampedIndex = targetIndex.coerceIn(0, _apps.value.size - 1)
            _dragState.value = state.copy(currentIndex = clampedIndex)
        }
    }

    fun endDrag() {
        _dragState.value?.let { state ->
            if (state.draggedIndex != state.currentIndex) {
                moveApp(state.draggedIndex, state.currentIndex)
            }
        }
        _dragState.value = null
    }

    fun cancelDrag() {
        _dragState.value = null
    }

    fun getAppIndex(appId: String): Int {
        return _apps.value.indexOfFirst { it.id == appId }
    }

    fun toggleSortMode() {
        _sortMode.value = when (_sortMode.value) {
            SortMode.MANUAL -> SortMode.NAME_ASC
            SortMode.NAME_ASC -> SortMode.NAME_DESC
            SortMode.NAME_DESC -> SortMode.MANUAL
        }
        updateDerived()
    }

    fun setTagFilter(tag: String?) {
        _selectedTag.value = tag
        updateDerived()
    }

    fun renameTag(oldTag: String, newTag: String) {
        if (oldTag == newTag || newTag.isBlank()) return
        _apps.value = _apps.value.map { app ->
            if (app.tags.contains(oldTag)) {
                app.copy(tags = app.tags.map { if (it == oldTag) newTag else it }.distinct())
            } else app
        }
        saveApps()
        updateDerived()
    }

    fun deleteTag(tag: String) {
        _apps.value = _apps.value.map { app ->
            if (app.tags.contains(tag)) {
                app.copy(tags = app.tags.filter { it != tag })
            } else app
        }
        if (_selectedTag.value == tag) {
            _selectedTag.value = null
        }
        saveApps()
        updateDerived()
    }
}

data class DragState(
    val draggedId: String,
    val draggedIndex: Int,
    val currentIndex: Int
)
