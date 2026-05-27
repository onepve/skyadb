package com.sky22333.skyadb.ui.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sky22333.skyadb.model.OperationStatus
import com.sky22333.skyadb.ui.components.SectionHeader
import com.sky22333.skyadb.ui.theme.AdbManagerTheme
import com.sky22333.skyadb.ui.theme.AppDimens

@Composable
fun PairingScreen(
    onBackClick: () -> Unit,
    discoveredHost: String = "",
    discoveredPort: String = "",
    onDiscoveredEndpointConsumed: () -> Unit = {},
    viewModel: PairingViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(discoveredHost, discoveredPort) {
        val port = discoveredPort.toIntOrNull()
        if (discoveredHost.isNotBlank() && port != null) {
            viewModel.onDiscoveredEndpointSelected(discoveredHost, port)
            onDiscoveredEndpointConsumed()
        }
    }

    PairingContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onIpChanged = viewModel::onIpChanged,
        onPairingPortChanged = viewModel::onPairingPortChanged,
        onPairingCodeChanged = viewModel::onPairingCodeChanged,
        onPairClick = viewModel::onPairClicked,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PairingContent(
    uiState: PairingUiState,
    onBackClick: () -> Unit,
    onIpChanged: (String) -> Unit,
    onPairingPortChanged: (String) -> Unit,
    onPairingCodeChanged: (String) -> Unit,
    onPairClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(text = "无线调试配对")
                    Text(
                        text = "用于 Android 11 及以上设备",
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
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SectionGap),
        ) {
            PairingGuideCard()
            PairingFormCard(
                uiState = uiState,
                onIpChanged = onIpChanged,
                onPairingPortChanged = onPairingPortChanged,
                onPairingCodeChanged = onPairingCodeChanged,
                onPairClick = onPairClick,
            )
        }
    }
}

@Composable
private fun PairingGuideCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeader(
                title = "配对前检查",
                description = "目标设备需开启无线调试，并显示配对码",
            )
            GuideStep("1", "在目标设备打开开发者选项和无线调试。")
            GuideStep("2", "选择“使用配对码配对设备”，记录 IP、配对端口和配对码。")
            GuideStep("3", "确认本机和目标设备处于同一网络，再开始配对。")
        }
    }
}

@Composable
private fun GuideStep(index: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = index,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PairingFormCard(
    uiState: PairingUiState,
    onIpChanged: (String) -> Unit,
    onPairingPortChanged: (String) -> Unit,
    onPairingCodeChanged: (String) -> Unit,
    onPairClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimens.CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(AppDimens.CardPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionHeader(
                title = "配对信息",
                description = "配对端口和连接端口不同，请填写配对窗口中显示的端口",
            )

            OutlinedTextField(
                value = uiState.ip,
                onValueChange = onIpChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("配对 IP") },
                singleLine = true,
                placeholder = { Text("例如 192.168.1.86") },
                isError = uiState.ipError != null,
                supportingText = {
                    Text(uiState.ipError ?: "填写目标设备无线调试配对窗口中的 IP")
                },
            )

            OutlinedTextField(
                value = uiState.pairingPort,
                onValueChange = onPairingPortChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("配对端口") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.portError != null,
                supportingText = {
                    Text(uiState.portError ?: "填写配对窗口中的临时端口")
                },
            )

            OutlinedTextField(
                value = uiState.pairingCode,
                onValueChange = onPairingCodeChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("配对码") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = uiState.codeError != null,
                supportingText = {
                    Text(uiState.codeError ?: "输入目标设备显示的 6 位数字配对码")
                },
            )

            PairingStatusMessage(status = uiState.operationStatus)

            Button(
                onClick = onPairClick,
                enabled = uiState.pairEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Outlined.Key, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "开始配对")
            }
        }
    }
}

@Composable
private fun PairingStatusMessage(status: OperationStatus) {
    when (status) {
        OperationStatus.Idle -> Unit
        is OperationStatus.Running -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = status.text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        is OperationStatus.Success -> Text(
            text = status.text,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodySmall,
        )
        is OperationStatus.Failed -> Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimens.CardRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = status.text,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = status.suggestion,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Preview(name = "无线配对 - 空状态", showBackground = true, widthDp = 390)
@Composable
private fun PairingContentEmptyPreview() {
    AdbManagerTheme(dynamicColor = false) {
        PairingContent(
            uiState = PairingUiState(),
            onBackClick = {},
            onIpChanged = {},
            onPairingPortChanged = {},
            onPairingCodeChanged = {},
            onPairClick = {},
        )
    }
}

@Preview(name = "无线配对 - 错误态", showBackground = true, widthDp = 390)
@Composable
private fun PairingContentErrorPreview() {
    AdbManagerTheme(dynamicColor = false) {
        PairingContent(
            uiState = PairingUiState(
                ip = "192.168.1.999",
                pairingPort = "70000",
                pairingCode = "12",
                ipError = "请输入正确的 IPv4 地址",
                portError = "配对端口范围应为 1-65535",
                codeError = "配对码通常为 6 位数字",
                operationStatus = OperationStatus.Failed(
                    text = "无法发起配对",
                    suggestion = "请检查配对 IP、配对端口和 6 位配对码是否正确。",
                ),
            ),
            onBackClick = {},
            onIpChanged = {},
            onPairingPortChanged = {},
            onPairingCodeChanged = {},
            onPairClick = {},
        )
    }
}

@Preview(name = "无线配对 - 准备中", showBackground = true, widthDp = 390)
@Composable
private fun PairingContentRunningPreview() {
    AdbManagerTheme(dynamicColor = false) {
        PairingContent(
            uiState = PairingUiState(
                ip = "192.168.1.86",
                pairingPort = "37125",
                pairingCode = "123456",
                pairEnabled = true,
                operationStatus = OperationStatus.Running("正在准备配对 192.168.1.86:37125"),
            ),
            onBackClick = {},
            onIpChanged = {},
            onPairingPortChanged = {},
            onPairingCodeChanged = {},
            onPairClick = {},
        )
    }
}
