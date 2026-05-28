package com.sky22333.skyadb.ui.files

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.sky22333.skyadb.ui.components.AppTopBar as TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky22333.skyadb.model.OperationStatus
import com.sky22333.skyadb.model.RemoteFileEntry
import com.sky22333.skyadb.ui.components.EmptyState
import com.sky22333.skyadb.ui.components.SectionHeader
import com.sky22333.skyadb.ui.theme.AdbManagerTheme
import com.sky22333.skyadb.ui.theme.AppDimens
import java.util.Locale

@Composable
fun FileTransferScreen(
    bottomPadding: Dp = 0.dp,
    onBackClick: () -> Unit,
    viewModel: FileTransferViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var downloadEntry by remember { mutableStateOf<RemoteFileEntry?>(null) }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.onLocalFileSelected(uri)
    }
    val createFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val entry = downloadEntry
        downloadEntry = null
        if (entry != null) {
            viewModel.downloadToUri(context, entry, uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadCurrentPath()
    }

    FileManagerContent(
        bottomPadding = bottomPadding,
        uiState = uiState,
        onBackClick = onBackClick,
        onRefreshClick = viewModel::loadCurrentPath,
        onPathChanged = viewModel::onPathInputChanged,
        onJumpClick = viewModel::jumpToPath,
        onGoUpClick = viewModel::goUp,
        onUploadClick = { openFile.launch(arrayOf("*/*")) },
        onNewFolderClick = viewModel::showNewFolderDialog,
        onOpenEntry = viewModel::openEntry,
        onDownloadEntry = { entry ->
            downloadEntry = entry
            createFile.launch(entry.name)
        },
        onDeleteEntry = viewModel::requestDelete,
        onCancelDelete = viewModel::cancelDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissNewFolder = viewModel::dismissNewFolderDialog,
        onCreateFolder = viewModel::createFolder,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileManagerContent(
    bottomPadding: Dp = 0.dp,
    uiState: FileTransferUiState,
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onPathChanged: (String) -> Unit,
    onJumpClick: () -> Unit,
    onGoUpClick: () -> Unit,
    onUploadClick: () -> Unit,
    onNewFolderClick: () -> Unit,
    onOpenEntry: (RemoteFileEntry) -> Unit,
    onDownloadEntry: (RemoteFileEntry) -> Unit,
    onDeleteEntry: (RemoteFileEntry) -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissNewFolder: () -> Unit,
    onCreateFolder: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("文件管理")
                    Text(
                        "浏览目标设备文件，上传或下载文件",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                IconButton(onClick = onRefreshClick, enabled = !uiState.loading) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "刷新目录")
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = AppDimens.ScreenPadding,
                top = AppDimens.ScreenPadding,
                end = AppDimens.ScreenPadding,
                bottom = AppDimens.ScreenPadding + bottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SectionGap),
        ) {
            item {
                PathCard(
                    uiState = uiState,
                    onPathChanged = onPathChanged,
                    onJumpClick = onJumpClick,
                    onGoUpClick = onGoUpClick,
                    onUploadClick = onUploadClick,
                    onNewFolderClick = onNewFolderClick,
                )
            }
            item { FileManagerStatus(status = uiState.operationStatus, loading = uiState.loading) }
            item {
                SectionHeader(
                    title = "文件列表",
                    description = "${uiState.currentPath} · ${uiState.entries.size} 个项目",
                )
            }
            if (!uiState.loading && uiState.entries.isEmpty()) {
                item {
                    EmptyState(
                        title = "目录为空",
                        message = "可以上传文件，或跳转到其他设备目录。",
                    )
                }
            } else {
                items(uiState.entries, key = { it.path }) { entry ->
                    RemoteFileCard(
                        entry = entry,
                        onOpenEntry = onOpenEntry,
                        onDownloadEntry = onDownloadEntry,
                        onDeleteEntry = onDeleteEntry,
                    )
                }
            }
        }
    }

    DeleteConfirmDialog(
        entry = uiState.pendingDelete,
        onDismiss = onCancelDelete,
        onConfirm = onConfirmDelete,
    )
    NewFolderDialog(
        visible = uiState.newFolderDialogVisible,
        onDismiss = onDismissNewFolder,
        onCreate = onCreateFolder,
    )
}

@Composable
private fun PathCard(
    uiState: FileTransferUiState,
    onPathChanged: (String) -> Unit,
    onJumpClick: () -> Unit,
    onGoUpClick: () -> Unit,
    onUploadClick: () -> Unit,
    onNewFolderClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(title = "当前位置", description = "目录路径需要以 / 开头")
            OutlinedTextField(
                value = uiState.pathInput,
                onValueChange = onPathChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("设备目录") },
                singleLine = true,
                isError = uiState.pathError != null,
                supportingText = { Text(uiState.pathError ?: "例如 /sdcard/Download") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onGoUpClick,
                    enabled = uiState.canGoUp && !uiState.loading,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("上级")
                }
                Button(
                    onClick = onJumpClick,
                    enabled = !uiState.loading,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("跳转")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onUploadClick, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("上传")
                }
                OutlinedButton(onClick = onNewFolderClick, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("新建文件夹")
                }
            }
        }
    }
}

@Composable
private fun RemoteFileCard(
    entry: RemoteFileEntry,
    onOpenEntry: (RemoteFileEntry) -> Unit,
    onDownloadEntry: (RemoteFileEntry) -> Unit,
    onDeleteEntry: (RemoteFileEntry) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        onClick = { onOpenEntry(entry) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 62.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (entry.isDirectory) Icons.Outlined.Folder else Icons.AutoMirrored.Outlined.InsertDriveFile,
                contentDescription = null,
                tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (entry.isDirectory) "文件夹" else formatBytes(entry.sizeBytes),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "文件操作")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    if (!entry.isDirectory) {
                        DropdownMenuItem(
                            text = { Text("下载") },
                            leadingIcon = { Icon(Icons.Outlined.Download, contentDescription = null) },
                            onClick = {
                                expanded = false
                                onDownloadEntry(entry)
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("删除") },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                        onClick = {
                            expanded = false
                            onDeleteEntry(entry)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FileManagerStatus(status: OperationStatus, loading: Boolean) {
    when (status) {
        OperationStatus.Idle -> Unit
        is OperationStatus.Running -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(status.text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        is OperationStatus.Success -> Text(status.text, color = MaterialTheme.colorScheme.primary)
        is OperationStatus.Failed -> Text(
            text = "${status.text}：${status.suggestion}",
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun DeleteConfirmDialog(
    entry: RemoteFileEntry?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (entry == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除 ${entry.name}？") },
        text = { Text(if (entry.isDirectory) "仅支持删除空文件夹。" else "该操作会从目标设备删除此文件。") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("删除") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun NewFolderDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    if (!visible) return
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建文件夹") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("文件夹名称") },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { onCreate(name) }) { Text("创建") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.US, "%.1f MB", mb)
}

@Preview(name = "文件管理", showBackground = true, widthDp = 390)
@Composable
private fun FileManagerContentPreview() {
    AdbManagerTheme(dynamicColor = false) {
        FileManagerContent(
            uiState = FileTransferUiState(
                entries = listOf(
                    RemoteFileEntry("Pictures", "/sdcard/Download/Pictures", true, 0L),
                    RemoteFileEntry("config.json", "/sdcard/Download/config.json", false, 2048L),
                ),
            ),
            onBackClick = {},
            onRefreshClick = {},
            onPathChanged = {},
            onJumpClick = {},
            onGoUpClick = {},
            onUploadClick = {},
            onNewFolderClick = {},
            onOpenEntry = {},
            onDownloadEntry = {},
            onDeleteEntry = {},
            onCancelDelete = {},
            onConfirmDelete = {},
            onDismissNewFolder = {},
            onCreateFolder = {},
        )
    }
}
