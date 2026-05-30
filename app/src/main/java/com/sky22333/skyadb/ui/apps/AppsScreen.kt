package com.sky22333.skyadb.ui.apps

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.sky22333.skyadb.ui.components.AppTopBar as TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky22333.skyadb.apps.AppMetadata
import com.sky22333.skyadb.model.AppInfo
import com.sky22333.skyadb.model.OperationStatus
import com.sky22333.skyadb.ui.components.EmptyState
import com.sky22333.skyadb.ui.components.SectionHeader
import com.sky22333.skyadb.ui.theme.AdbManagerTheme
import com.sky22333.skyadb.ui.theme.AppDimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppsScreen(
    bottomPadding: Dp = 0.dp,
    onBackClick: () -> Unit,
    viewModel: AppsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadApps()
    }

    AppsContent(
        bottomPadding = bottomPadding,
        uiState = uiState,
        onBackClick = onBackClick,
        onQueryChanged = viewModel::onQueryChanged,
        onFilterChanged = viewModel::onFilterChanged,
        onRefreshClick = { viewModel.loadApps(force = true) },
        onLaunchClick = viewModel::launchApp,
        onStopClick = viewModel::forceStopApp,
        onSetEnabledClick = viewModel::setAppEnabled,
        onAppVisible = viewModel::loadAppMetadata,
        onAppHidden = viewModel::cancelAppMetadataLoad,
        onUninstallClick = viewModel::uninstallApp,
        onCancelPendingAction = viewModel::cancelPendingAction,
        onConfirmPendingAction = viewModel::confirmPendingAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppsContent(
    bottomPadding: Dp = 0.dp,
    uiState: AppsUiState,
    onBackClick: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onFilterChanged: (AppFilter) -> Unit,
    onRefreshClick: () -> Unit,
    onLaunchClick: (String) -> Unit,
    onStopClick: (String) -> Unit,
    onSetEnabledClick: (AppInfo, Boolean) -> Unit,
    onAppVisible: (AppInfo) -> Unit,
    onAppHidden: (String) -> Unit,
    onUninstallClick: (String) -> Unit,
    onCancelPendingAction: () -> Unit,
    onConfirmPendingAction: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(text = "应用管理")
                    Text(
                        text = "查看、启动、停止或卸载应用",
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
                IconButton(onClick = onRefreshClick) {
                    Icon(imageVector = Icons.Outlined.Refresh, contentDescription = "刷新应用列表")
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
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜索应用") },
                    singleLine = true,
                    placeholder = { Text("输入包名或应用名") },
                )
            }
            item {
                AppFilterRow(
                    selected = uiState.filter,
                    apps = uiState.apps,
                    onFilterChanged = onFilterChanged,
                )
            }
            item {
                PendingActionCard(
                    pendingAction = uiState.pendingAction,
                    onCancelClick = onCancelPendingAction,
                    onConfirmClick = onConfirmPendingAction,
                )
            }
            item { AppsStatusMessage(status = uiState.operationStatus) }
            item {
                SectionHeader(
                    title = "应用列表",
                    description = "共 ${uiState.filteredApps.size} 个，分类和搜索在本地即时过滤",
                )
            }
            if (uiState.loading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }
            if (!uiState.loading && uiState.filteredApps.isEmpty()) {
                item { EmptyState(title = "暂无应用", message = "连接设备并刷新后，应用会显示在这里。") }
            } else {
                items(
                    items = uiState.filteredApps,
                    key = { it.packageName },
                ) { app ->
                    AppItemCard(
                        app = app,
                        metadata = uiState.appMetadata[app.packageName],
                        onLaunchClick = onLaunchClick,
                        onStopClick = onStopClick,
                        onSetEnabledClick = onSetEnabledClick,
                        onAppVisible = onAppVisible,
                        onAppHidden = onAppHidden,
                        onUninstallClick = onUninstallClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppFilterRow(
    selected: AppFilter,
    apps: List<AppInfo>,
    onFilterChanged: (AppFilter) -> Unit,
) {
    val userCount = apps.count { !it.isSystem }
    val systemCount = apps.count { it.isSystem }
    val counts = mapOf(
        AppFilter.All to apps.size,
        AppFilter.User to userCount,
        AppFilter.System to systemCount,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppFilter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onFilterChanged(filter) },
                label = { Text("${filter.label} ${counts[filter] ?: 0}") },
            )
        }
    }
}

@Composable
private fun PendingActionCard(
    pendingAction: AppPendingAction?,
    onCancelClick: () -> Unit,
    onConfirmClick: () -> Unit,
) {
    if (pendingAction == null) return

    val title = when (pendingAction) {
        is AppPendingAction.Uninstall -> "确认卸载应用？"
        is AppPendingAction.SetEnabled -> if (pendingAction.enabled) "确认启用应用？" else "确认冻结应用？"
    }
    val message = when (pendingAction) {
        is AppPendingAction.Uninstall -> "将从目标设备卸载 ${pendingAction.packageName}。"
        is AppPendingAction.SetEnabled -> when {
            pendingAction.enabled -> "将恢复 ${pendingAction.packageName} 的可用状态。"
            pendingAction.isSystem -> "冻结系统应用可能影响桌面、设置或播放功能。"
            else -> "冻结后 ${pendingAction.packageName} 将无法启动。"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancelClick) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("取消")
                }
                TextButton(onClick = onConfirmClick) {
                    Icon(imageVector = Icons.Outlined.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("确认")
                }
            }
        }
    }
}

@Composable
private fun AppItemCard(
    app: AppInfo,
    metadata: AppMetadata?,
    onLaunchClick: (String) -> Unit,
    onStopClick: (String) -> Unit,
    onSetEnabledClick: (AppInfo, Boolean) -> Unit,
    onAppVisible: (AppInfo) -> Unit,
    onAppHidden: (String) -> Unit,
    onUninstallClick: (String) -> Unit,
) {
    LaunchedEffect(app.packageName, app.sourcePath) {
        onAppVisible(app)
    }
    DisposableEffect(app.packageName) {
        onDispose { onAppHidden(app.packageName) }
    }

    Card(
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
            AppIconPlaceholder(app = app, iconPath = metadata?.iconPath)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metadata?.label?.takeIf { it.isNotBlank() } ?: app.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    text = app.packageName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
            AssistChip(
                onClick = {},
                label = { Text(app.statusLabel) },
            )
            AppActionMenu(
                app = app,
                onLaunchClick = onLaunchClick,
                onStopClick = onStopClick,
                onSetEnabledClick = onSetEnabledClick,
                onUninstallClick = onUninstallClick,
            )
        }
    }
}

@Composable
private fun AppIconPlaceholder(app: AppInfo, iconPath: String?) {
    val iconBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = iconPath) {
        value = withContext(Dispatchers.IO) {
            iconPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
        }
    }
    Card(
        modifier = Modifier.size(36.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isSystem) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val bitmap = iconBitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Android,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (app.isSystem) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                )
            }
        }
    }
}

@Composable
private fun AppActionMenu(
    app: AppInfo,
    onLaunchClick: (String) -> Unit,
    onStopClick: (String) -> Unit,
    onSetEnabledClick: (AppInfo, Boolean) -> Unit,
    onUninstallClick: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = "应用操作",
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("启动") },
                leadingIcon = { Icon(Icons.Outlined.PlayArrow, contentDescription = null) },
                enabled = app.enabled,
                onClick = {
                    expanded = false
                    onLaunchClick(app.packageName)
                },
            )
            DropdownMenuItem(
                text = { Text("停止") },
                leadingIcon = { Icon(Icons.Outlined.StopCircle, contentDescription = null) },
                enabled = app.enabled,
                onClick = {
                    expanded = false
                    onStopClick(app.packageName)
                },
            )
            DropdownMenuItem(
                text = { Text(if (app.enabled) "冻结" else "启用") },
                leadingIcon = {
                    Icon(
                        imageVector = if (app.enabled) Icons.Outlined.Close else Icons.Outlined.Check,
                        contentDescription = null,
                    )
                },
                onClick = {
                    expanded = false
                    onSetEnabledClick(app, !app.enabled)
                },
            )
            DropdownMenuItem(
                text = { Text("卸载") },
                leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onUninstallClick(app.packageName)
                },
            )
        }
    }
}

private val AppInfo.statusLabel: String
    get() = when {
        !enabled && isSystem -> "系统冻结"
        !enabled -> "已冻结"
        isSystem -> "系统"
        else -> "用户"
    }

@Composable
private fun AppsStatusMessage(status: OperationStatus) {
    when (status) {
        OperationStatus.Idle -> Unit
        is OperationStatus.Running -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        is OperationStatus.Success -> Text(text = status.text, color = MaterialTheme.colorScheme.primary)
        is OperationStatus.Failed -> Text(
            text = "${status.text}：${status.suggestion}",
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Preview(name = "应用管理 - 列表", showBackground = true, widthDp = 390)
@Composable
private fun AppsContentPreview() {
    AdbManagerTheme(dynamicColor = false) {
        AppsContent(
            uiState = AppsUiState(
                apps = listOf(
                    AppInfo("com.android.tv.settings", "settings", true),
                    AppInfo("com.example.player", "player", false),
                ),
                operationStatus = OperationStatus.Success("已读取 2 个应用"),
            ),
            onBackClick = {},
            onQueryChanged = {},
            onFilterChanged = {},
            onRefreshClick = {},
            onLaunchClick = {},
            onStopClick = {},
            onSetEnabledClick = { _, _ -> },
            onAppVisible = {},
            onAppHidden = {},
            onUninstallClick = {},
            onCancelPendingAction = {},
            onConfirmPendingAction = {},
        )
    }
}
