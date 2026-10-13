package com.example.updater

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.model.AppUpdateInfo
import com.example.model.DownloadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

class AppUpdateManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _canInstallUnknown = MutableStateFlow(checkUnknownSourcesPermission())
    val canInstallUnknown: StateFlow<Boolean> = _canInstallUnknown.asStateFlow()

    private var activeDownloadJob: Job? = null

    val okHttpClient: OkHttpClient by lazy {
        APKDownloader.createDefaultOkHttpClient()
    }

    val apkDownloader: APKDownloader by lazy {
        APKDownloader(okHttpClient)
    }

    fun refreshPermissionStatus(): Boolean {
        _canInstallUnknown.value = true
        return true
    }

    private fun checkUnknownSourcesPermission(): Boolean {
        return true
    }

    fun openInstallUnknownAppsSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }

    fun cancelDownload() {
        activeDownloadJob?.cancel()
        activeDownloadJob = null
        apkDownloader.cancelDownload()
        _downloadState.value = DownloadState.Idle
    }

    fun startDownload(updateInfo: AppUpdateInfo) {
        activeDownloadJob?.cancel()
        activeDownloadJob = scope.launch {
            try {
                _downloadState.value = DownloadState.Downloading(
                    progress = 0f,
                    downloadedBytes = 0L,
                    totalBytes = updateInfo.fileSizeBytes,
                    speed = "Starting...",
                    percentage = 0
                )

                val updateDir = File(context.externalCacheDir ?: context.cacheDir, "updates").apply { mkdirs() }
                val downloadUrl = updateInfo.downloadUrl
                val fileName = downloadUrl.substringAfterLast("/").takeIf { it.endsWith(".apk", ignoreCase = true) } 
                    ?: "update_${updateInfo.latestVersionCode}.apk"
                val targetFile = File(updateDir, fileName)

                apkDownloader.downloadAsFlow(
                    url = downloadUrl,
                    destinationFile = targetFile,
                    fallbackContentLength = updateInfo.fileSizeBytes
                ).collect { progress ->
                    when (progress) {
                        is DownloadProgress.Starting -> {
                            _downloadState.value = DownloadState.Downloading(
                                progress = 0f,
                                downloadedBytes = 0L,
                                totalBytes = updateInfo.fileSizeBytes,
                                speed = "Connecting...",
                                percentage = 0
                            )
                        }
                        is DownloadProgress.Progress -> {
                            _downloadState.value = DownloadState.Downloading(
                                progress = progress.progressFraction,
                                downloadedBytes = progress.bytesDownloaded,
                                totalBytes = progress.totalBytes,
                                speed = progress.speedFormatted,
                                percentage = progress.percentage
                            )
                        }
                        is DownloadProgress.Success -> {
                            try {
                                val publicDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                if (publicDownloadsDir != null && publicDownloadsDir.exists()) {
                                    val publicApk = File(publicDownloadsDir, fileName)
                                    progress.file.copyTo(publicApk, overwrite = true)
                                    publicApk.setReadable(true, false)
                                }
                            } catch (_: Exception) {}

                            _downloadState.value = DownloadState.Downloaded(progress.file)
                        }
                        is DownloadProgress.Failure -> {
                            _downloadState.value = DownloadState.Error(progress.errorMessage)
                        }
                        is DownloadProgress.Cancelled -> {
                            _downloadState.value = DownloadState.Idle
                        }
                    }
                }
            } catch (e: CancellationException) {
                _downloadState.value = DownloadState.Idle
            } catch (e: Exception) {
                _downloadState.value = DownloadState.Error(e.localizedMessage ?: "Download failed")
            }
        }
    }

    fun triggerInstall(file: File, updateInfo: AppUpdateInfo): Boolean {
        val hasPermission = checkUnknownSourcesPermission()
        _canInstallUnknown.value = hasPermission

        if (!hasPermission) {
            return false
        }

        try {
            _downloadState.value = DownloadState.Installing
            try {
                file.setReadable(true, false)
            } catch (_: Exception) {}

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            try {
                val resInfoList = context.packageManager.queryIntentActivities(
                    installIntent,
                    PackageManager.MATCH_DEFAULT_ONLY
                )
                for (resolveInfo in resInfoList) {
                    val pkgName = resolveInfo.activityInfo.packageName
                    context.grantUriPermission(
                        pkgName,
                        apkUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            } catch (_: Exception) {}

            try {
                context.startActivity(installIntent)
                return true
            } catch (e: Exception) {
                try {
                    val downloadsIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(downloadsIntent)
                    return true
                } catch (_: Exception) {
                    val openFilesIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(openFilesIntent)
                    return true
                }
            }
        } catch (e: Exception) {
            _downloadState.value = DownloadState.Error("Installer launch failed: ${e.localizedMessage}")
            return false
        }
    }

    fun openAppDetailsSettings(packageName: String? = null) {
        try {
            val targetPkg = packageName ?: context.packageName
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", targetPkg, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun openPlayProtectSettings() {
        try {
            val intent = Intent().apply {
                setClassName("com.google.android.gms", "com.google.android.gms.security.settings.VerifyAppsSettingsActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {}
        }
    }

    fun resetState() {
        _downloadState.value = DownloadState.Idle
    }
}
