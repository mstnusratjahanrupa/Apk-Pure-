package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.PlayProtectAndRestrictedSettingsDialog
import com.example.ui.components.SecurityPermissionDialog
import com.example.ui.components.TimeForUpdatesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.UpdaterViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CleanAppUpdaterView()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanAppUpdaterView(
    viewModel: UpdaterViewModel = viewModel()
) {
    val updateInfo by viewModel.updateInfo.collectAsState()
    val flowStep by viewModel.flowStep.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val showSecurityDialog by viewModel.showSecurityDialog.collectAsState()
    val showSecurityGuideDialog by viewModel.showSecurityGuideDialog.collectAsState()

    // Resume listener to automatically refresh unknown apps install permission
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Google Play Store WhatsApp screen
    TimeForUpdatesScreen(
        updateInfo = updateInfo,
        flowStep = flowStep,
        downloadState = downloadState,
        onUpdateClick = { viewModel.onUpdateClicked() },
        onInstallClick = { viewModel.attemptInstallApk() },
        onCancelDownload = { viewModel.cancelDownload() },
        onRetryDownload = { viewModel.retryDownload() },
        onOpenSecurityGuide = { viewModel.openSecurityGuide() }
    )

    // Unknown Sources Security Dialog (only if install unknown apps is needed upon installing)
    if (showSecurityDialog) {
        SecurityPermissionDialog(
            appName = updateInfo.appName,
            onDismiss = { viewModel.onSecurityDialogDismissed() },
            onGoToSettings = {
                viewModel.onSecurityDialogSettingsClicked()
                viewModel.onSecurityPermissionGrantedOrContinued()
            }
        )
    }

    // Play Protect & Restricted Settings Safety Guide
    if (showSecurityGuideDialog) {
        PlayProtectAndRestrictedSettingsDialog(
            appName = updateInfo.appName,
            onDismiss = { viewModel.dismissSecurityGuide() },
            onOpenAppInfo = { viewModel.openAppInfoSettings() },
            onOpenPlayProtectSettings = { viewModel.openPlayProtectSettings() }
        )
    }
}
