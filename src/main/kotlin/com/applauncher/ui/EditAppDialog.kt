package com.applauncher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditAppDialog(
    entry: AppEntry?,
    allTags: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (AppEntry) -> Unit
) {
    var name by remember(entry) { mutableStateOf(entry?.name ?: "") }
    var path by remember(entry) { mutableStateOf(entry?.path ?: "") }
    var arguments by remember(entry) { mutableStateOf(entry?.arguments ?: "") }
    var workingDirectory by remember(entry) { mutableStateOf(entry?.workingDirectory ?: "") }
    var tags by remember(entry) { mutableStateOf(entry?.tags ?: emptyList()) }
    var runAsAdmin by remember(entry) { mutableStateOf(entry?.runAsAdmin ?: false) }
    var newTagText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (entry == null) "アプリを追加" else "アプリを編集")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
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

                // Admin toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "管理者として起動",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = runAsAdmin,
                        onCheckedChange = { runAsAdmin = it }
                    )
                }

                // Tags section
                Text(
                    text = "タグ",
                    style = MaterialTheme.typography.labelLarge
                )

                // Current tags as InputChips
                if (tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tags.forEach { tag ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text(tag) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { tags = tags.filter { it != tag } },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove tag",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // New tag input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        label = { Text("新しいタグ") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            val trimmed = newTagText.trim()
                            if (trimmed.isNotBlank() && !tags.contains(trimmed)) {
                                tags = tags + trimmed
                                newTagText = ""
                            }
                        },
                        enabled = newTagText.trim().isNotBlank() && !tags.contains(newTagText.trim())
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add tag")
                    }
                }

                // Suggestion chips for existing tags not yet assigned
                val suggestions = allTags.filter { it !in tags }
                if (suggestions.isNotEmpty()) {
                    Text(
                        text = "既存のタグから選択",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        suggestions.forEach { tag ->
                            SuggestionChip(
                                onClick = { tags = tags + tag },
                                label = { Text(tag) }
                            )
                        }
                    }
                }
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
                            workingDirectory = workingDirectory,
                            tags = tags,
                            runAsAdmin = runAsAdmin
                        ) ?: AppEntry(
                            name = name,
                            path = path,
                            arguments = arguments,
                            workingDirectory = workingDirectory,
                            tags = tags,
                            runAsAdmin = runAsAdmin
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
