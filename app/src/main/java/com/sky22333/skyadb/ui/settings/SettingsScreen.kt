package com.sky22333.skyadb.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky22333.skyadb.data.ThemeMode
import com.sky22333.skyadb.ui.components.SectionHeader
import com.sky22333.skyadb.ui.theme.AppDimens
import com.sky22333.skyadb.ui.theme.AdbManagerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsContent(
        uiState = uiState,
        onDefaultPortChanged = viewModel::onDefaultPortChanged,
        onConnectionTimeoutChanged = viewModel::onConnectionTimeoutChanged,
        onCommandTimeoutChanged = viewModel::onCommandTimeoutChanged,
        onScanRangesChanged = viewModel::onScanRangesChanged,
        onThemeModeSelected = viewModel::onThemeModeSelected,
        onClearRecentDevicesClicked = viewModel::onClearRecentDevicesClicked,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onDefaultPortChanged: (String) -> Unit,
    onConnectionTimeoutChanged: (String) -> Unit,
    onCommandTimeoutChanged: (String) -> Unit,
    onScanRangesChanged: (String) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onClearRecentDevicesClicked: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("设置") })

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = AppDimens.ScreenPadding, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SectionHeader(title = "连接参数", description = "修改后自动保存到本机") }
            item {
                NumberSettingItem(
                    icon = Icons.Outlined.SettingsEthernet,
                    title = "默认 ADB 端口",
                    value = uiState.defaultPort,
                    suffix = "端口",
                    error = uiState.defaultPortError,
                    onValueChanged = onDefaultPortChanged,
                )
            }
            item {
                NumberSettingItem(
                    icon = Icons.Outlined.Schedule,
                    title = "连接超时",
                    value = uiState.connectionTimeoutSeconds,
                    suffix = "秒",
                    error = uiState.connectionTimeoutError,
                    onValueChanged = onConnectionTimeoutChanged,
                )
            }
            item {
                NumberSettingItem(
                    icon = Icons.Outlined.Schedule,
                    title = "命令超时",
                    value = uiState.commandTimeoutSeconds,
                    suffix = "秒",
                    error = uiState.commandTimeoutError,
                    onValueChanged = onCommandTimeoutChanged,
                )
            }
            item {
                TextSettingItem(
                    icon = Icons.Outlined.SettingsEthernet,
                    title = "扫描网段",
                    value = uiState.scanRanges,
                    placeholder = "例如 10.43.180.0/24",
                    description = "可填写多个网段或单个 IP，每行一个；默认使用当前网络 /24。",
                    error = uiState.scanRangesError,
                    onValueChanged = onScanRangesChanged,
                )
            }

            item { SectionHeader(title = "外观", description = "保持全 App 风格统一") }
            item {
                ThemeSettingItem(
                    icon = Icons.Outlined.DarkMode,
                    title = "主题模式",
                    selectedThemeMode = uiState.themeMode,
                    onThemeModeSelected = onThemeModeSelected,
                )
            }

            item { SectionHeader(title = "数据", description = "最近设备仅保存在本机") }
            item {
                ClearDataItem(
                    icon = Icons.Outlined.CleaningServices,
                    title = "清理最近设备",
                    description = "仅清理最近设备记录，其他设置不会受影响。",
                    statusText = uiState.statusText,
                    onClearRecentDevicesClicked = onClearRecentDevicesClicked,
                )
            }

            item { SectionHeader(title = "关于") }
            item {
                ProjectLinkItem(
                    icon = Icons.AutoMirrored.Outlined.OpenInNew,
                    title = "项目地址",
                    description = "skyadb",
                    onClick = { uriHandler.openUri(ProjectUrl) },
                )
            }
        }
    }
}

@Composable
private fun NumberSettingItem(
    icon: ImageVector,
    title: String,
    value: String,
    suffix: String,
    error: String?,
    onValueChanged: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    value = value,
                    onValueChange = onValueChanged,
                    singleLine = true,
                    suffix = { Text(suffix) },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                )
            }
        }
    }
}

@Composable
private fun TextSettingItem(
    icon: ImageVector,
    title: String,
    value: String,
    placeholder: String,
    description: String,
    error: String?,
    onValueChanged: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    value = value,
                    onValueChange = onValueChanged,
                    placeholder = { Text(placeholder) },
                    minLines = 1,
                    maxLines = 4,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                )
            }
        }
    }
}

@Composable
private fun ThemeSettingItem(
    icon: ImageVector,
    title: String,
    selectedThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedThemeMode == mode,
                            onClick = { onThemeModeSelected(mode) },
                            label = { Text(mode.label) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClearDataItem(
    icon: ImageVector,
    title: String,
    description: String,
    statusText: String,
    onClearRecentDevicesClicked: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = statusText,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(onClick = onClearRecentDevicesClicked) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = "清理最近设备",
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectLinkItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.labelLarge)
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private const val ProjectUrl = "https://github.com/sky22333/skyadb"

@Preview(name = "设置页", showBackground = true, widthDp = 390)
@Composable
private fun SettingsContentPreview() {
    AdbManagerTheme(dynamicColor = false) {
        SettingsContent(
            uiState = SettingsUiState(),
            onDefaultPortChanged = {},
            onConnectionTimeoutChanged = {},
            onCommandTimeoutChanged = {},
            onScanRangesChanged = {},
            onThemeModeSelected = {},
            onClearRecentDevicesClicked = {},
        )
    }
}
