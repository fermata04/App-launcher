package com.applauncher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.applauncher.model.AppEntry
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun EditAppDialog(
    entry: AppEntry?,
    onDismiss: () -> Unit,
    onSave: (AppEntry) -> Unit
) {
    var name by remember(entry) { mutableStateOf(entry?.name ?: "") }
    var path by remember(entry) { mutableStateOf(entry?.path ?: "") }
    var arguments by remember(entry) { mutableStateOf(entry?.arguments ?: "") }
    var workingDirectory by remember(entry) { mutableStateOf(entry?.workingDirectory ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (entry == null) "アプリを追加" else "アプリを編集")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("アプリ名") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("実行ファイルのパス") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            val chooser = JFileChooser().apply {
                                dialogTitle = "実行ファイルを選択"
                                fileFilter = FileNameExtensionFilter(
                                    "実行ファイル (*.exe, *.bat, *.cmd, *.lnk)",
                                    "exe", "bat", "cmd", "lnk"
                                )
                            }
                            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                path = chooser.selectedFile.absolutePath
                                if (name.isBlank()) {
                                    name = chooser.selectedFile.nameWithoutExtension
                                }
                                if (workingDirectory.isBlank()) {
                                    workingDirectory = chooser.selectedFile.parent ?: ""
                                }
                            }
                        }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Browse")
                        }
                    }
                )
                
                OutlinedTextField(
                    value = arguments,
                    onValueChange = { arguments = it },
                    label = { Text("引数（オプション）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = workingDirectory,
                    onValueChange = { workingDirectory = it },
                    label = { Text("作業ディレクトリ（オプション）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            val chooser = JFileChooser().apply {
                                dialogTitle = "作業ディレクトリを選択"
                                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                            }
                            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                                workingDirectory = chooser.selectedFile.absolutePath
                            }
                        }) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Browse")
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && path.isNotBlank()) {
                        val updatedEntry = entry?.copy(
                            name = name,
                            path = path,
                            arguments = arguments,
                            workingDirectory = workingDirectory
                        ) ?: AppEntry(
                            name = name,
                            path = path,
                            arguments = arguments,
                            workingDirectory = workingDirectory
                        )
                        onSave(updatedEntry)
                    }
                },
                enabled = name.isNotBlank() && path.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
