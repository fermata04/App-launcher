package com.applauncher.util

import java.awt.datatransfer.DataFlavor
import java.awt.dnd.*
import java.io.File
import javax.swing.JComponent

class DropTargetHandler(
    component: JComponent,
    private val onFilesDropped: (List<File>) -> Unit
) : DropTargetListener {
    
    init {
        DropTarget(component, DnDConstants.ACTION_COPY_OR_MOVE, this, true)
    }
    
    override fun dragEnter(dtde: DropTargetDragEvent) {
        if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            dtde.acceptDrag(DnDConstants.ACTION_COPY)
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
    
    override fun dragExit(dte: DropTargetEvent) {}
    
    override fun drop(dtde: DropTargetDropEvent) {
        try {
            dtde.acceptDrop(DnDConstants.ACTION_COPY)
            val transferable = dtde.transferable
            
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                @Suppress("UNCHECKED_CAST")
                val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                val executableFiles = files.filter { 
                    it.extension.lowercase() in listOf("exe", "bat", "cmd", "lnk", "msc")
                }
                if (executableFiles.isNotEmpty()) {
                    onFilesDropped(executableFiles)
                }
            }
            dtde.dropComplete(true)
        } catch (e: Exception) {
            e.printStackTrace()
            dtde.dropComplete(false)
        }
    }
}
