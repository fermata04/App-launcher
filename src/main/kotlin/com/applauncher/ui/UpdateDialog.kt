package com.applauncher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.applauncher.util.UpdateChecker
import com.applauncher.util.UpdateState

@Composable
fun UpdateDialog(
    updateState: UpdateState,
    onDismiss: () -> Unit,
    onStartDownload: () -> Unit,
    onInstallAndClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (updateState !is UpdateState.Downloading) {
                onDismiss()
            }
        },
        icon = {
            Icon(
                imageVector = when (updateState) {
                    is UpdateState.Available -> Icons.Default.NewReleases
                    is UpdateState.ReadyToInstall -> Icons.Default.CheckCircle
                    else -> Icons.Default.Download
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = when (updateState) {
                    is UpdateState.Available -> "アップデートがあります"
                    is UpdateState.Downloading -> "ダウンロード中..."
                    is UpdateState.ReadyToInstall -> "インストール準備完了"
                    is UpdateState.UpToDate -> "最新バージョンです"
                    is UpdateState.Error -> "エラー"
                    else -> "アップデート"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (updateState) {
                    is UpdateState.Available -> {
                        Text(
                            text = "新しいバージョン: ${updateState.release.tagName.removePrefix("v")}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "現在のバージョン: ${UpdateChecker.getCurrentVersion()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )

                        if (updateState.release.body.isNotBlank()) {
                            HorizontalDivider()
                            Text(
                                text = "変更内容:",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = updateState.release.body,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .heightIn(max = 150.dp)
                            )
                        }

                        Text(
                            text = "ファイル: ${updateState.asset.name} (${formatFileSize(updateState.asset.size)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    is UpdateState.Downloading -> {
                        if (updateState.progress >= 0f) {
                            LinearProgressIndicator(
                                progress = { updateState.progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "${formatFileSize(updateState.bytesRead)} / ${formatFileSize(updateState.totalBytes)}  (${(updateState.progress * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = "${formatFileSize(updateState.bytesRead)} ダウンロード済み",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    is UpdateState.ReadyToInstall -> {
                        Text(
                            text = "ダウンロードが完了しました。バックグラウンドでサイレントインストールを実行します。",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "インストール完了後、アプリが自動的に再起動します。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    is UpdateState.UpToDate -> {
                        Text(
                            text = "現在のバージョン ${UpdateChecker.getCurrentVersion()} は最新です。",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    is UpdateState.Error -> {
                        Text(
                            text = updateState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    else -> {}
                }
            }
        },
        confirmButton = {
            when (updateState) {
                is UpdateState.Available -> {
                    Button(onClick = onStartDownload) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ダウンロード")
                    }
                }
                is UpdateState.ReadyToInstall -> {
                    Button(onClick = onInstallAndClose) {
                        Text("インストール")
                    }
                }
                is UpdateState.Downloading -> {}
                else -> {
                    Button(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            }
        },
        dismissButton = {
            if (updateState !is UpdateState.Downloading) {
                TextButton(onClick = onDismiss) {
                    Text(if (updateState is UpdateState.Available) "後で" else "閉じる")
                }
            }
        }
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
        bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
        else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
    }
}
