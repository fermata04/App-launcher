package com.applauncher.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.layout.Layout
import com.applauncher.model.AppEntry
import com.applauncher.model.AppLauncherState
import com.applauncher.util.AppLogger
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.*
import java.io.File
import javax.swing.JPanel

// グローバルなドロップハンドラーの状態
object GlobalDropHandler {
    var onFilesDropped: ((List<File>) -> Unit)? = null
    var onDragEnter: (() -> Unit)? = null
    var onDragExit: (() -> Unit)? = null
}

@Composable
fun DropTargetArea(
    isActive: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    DisposableEffect(Unit) {
        GlobalDropHandler.onDragEnter = { onActiveChange(true) }
        GlobalDropHandler.onDragExit = { onActiveChange(false) }
        
        onDispose {
            GlobalDropHandler.onDragEnter = null
            GlobalDropHandler.onDragExit = null
        }
    }
    
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val width = placeables.maxOfOrNull { it.width } ?: constraints.minWidth
        val height = placeables.maxOfOrNull { it.height } ?: constraints.minHeight
        
        layout(width, height) {
            placeables.forEach { placeable ->
                placeable.placeRelative(0, 0)
            }
        }
    }
}

// AWTドロップターゲットの設定（ComposeWindowに適用）
fun setupWindowDropTarget(window: java.awt.Window, state: AppLauncherState) {
    val dropTarget = object : DropTargetListener {
        override fun dragEnter(dtde: DropTargetDragEvent) {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrag(DnDConstants.ACTION_COPY)
                GlobalDropHandler.onDragEnter?.invoke()
            } else {
                dtde.rejectDrag()
            }
        }
        
        override fun dragOver(dtde: DropTargetDragEvent) {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrag(DnDConstants.ACTION_COPY)
            }
        }
        
        override fun dropActionChanged(dtde: DropTargetDragEvent) {}
        
        override fun dragExit(dte: DropTargetEvent) {
            GlobalDropHandler.onDragExit?.invoke()
        }
        
        override fun drop(dtde: DropTargetDropEvent) {
            try {
                dtde.acceptDrop(DnDConstants.ACTION_COPY)
                val transferable = dtde.transferable
                
                if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    @Suppress("UNCHECKED_CAST")
                    val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                    val executableFiles = files.filter { 
                        it.extension.lowercase() in listOf("exe", "bat", "cmd", "lnk")
                    }
                    
                    executableFiles.forEach { file ->
                        val entry = AppEntry.fromPath(file.absolutePath)
                        state.addApp(entry)
                    }
                    
                    GlobalDropHandler.onFilesDropped?.invoke(executableFiles)
                }
                
                GlobalDropHandler.onDragExit?.invoke()
                dtde.dropComplete(true)
            } catch (e: Exception) {
                AppLogger.error("Failed to handle file drop", e)
                dtde.dropComplete(false)
                GlobalDropHandler.onDragExit?.invoke()
            }
        }
    }
    
    DropTarget(window, DnDConstants.ACTION_COPY_OR_MOVE, dropTarget, true)
}
