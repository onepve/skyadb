package com.sky22333.skyadb

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.sky22333.skyadb.data.ThemeMode
import com.sky22333.skyadb.ui.AdbManagerApp
import com.sky22333.skyadb.ui.theme.AdbManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by AppServices.settingsStore.settings.collectAsState(
                initial = com.sky22333.skyadb.data.AppSettings(),
            )
            val systemInDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.System -> systemInDarkTheme
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            LocalNetworkPermissionRequester()

            AdbManagerTheme(darkTheme = darkTheme) {
                AdbManagerApp()
            }
        }
    }
}

@Composable
private fun LocalNetworkPermissionRequester() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return
    val context = LocalContext.current
    val permission = Manifest.permission.ACCESS_LOCAL_NETWORK
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(permission)
        }
    }
}
