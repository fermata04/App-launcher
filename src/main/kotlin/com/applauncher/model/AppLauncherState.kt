package com.applauncher.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class AppLauncherState {
    private val configFile = File(System.getProperty("user.home"), ".applauncher/apps.json")
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()
    
    private val _dragState = MutableStateFlow<DragState?>(null)
    val dragState: StateFlow<DragState?> = _dragState.asStateFlow()
    
    init {
        loadApps()
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
        }
    }
    
    fun removeApp(id: String) {
        _apps.value = _apps.value.filter { it.id != id }
        saveApps()
    }
    
    fun updateApp(entry: AppEntry) {
        _apps.value = _apps.value.map { if (it.id == entry.id) entry else it }
        saveApps()
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
    }
    
    fun startDrag(appId: String) {
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
}

data class DragState(
    val draggedId: String,
    val draggedIndex: Int,
    val currentIndex: Int
)
