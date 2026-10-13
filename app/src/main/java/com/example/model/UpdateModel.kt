package com.example.model

import java.io.File

data class AppUpdateInfo(
    val appName: String = "Voice Assistant",
    val packageName: String = "com.mstnusratjahanrupa.voiceassistant",
    val currentVersionName: String = "1.0.0",
    val latestVersionName: String = "12.0.0",
    val latestVersionCode: Int = 1200,
    val releaseDate: String = "v12.0.0",
    val fileSizeBytes: Long = 32_000_000L,
    val formattedSize: String = "VoiceAssist v12.0 APK",
    val changelogTitle: String = "Time for UPDATES!",
    val changelogDescription: String = "We have added numerous new features and fixed some bugs to make the app as comfortable as possible for you",
    val downloadUrl: String = "https://github.com/installerapp-oss/Voice-assistant-/releases/download/Voice-assistant-12.00/VoiceAssist.apk"
)

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val speed: String,
        val percentage: Int = (progress * 100).toInt().coerceIn(0, 100)
    ) : DownloadState
    data class Downloaded(val file: File) : DownloadState
    data object Installing : DownloadState
    data class Error(val message: String) : DownloadState
}

enum class UpdateFlowStep {
    IDLE,
    STORAGE_PERMISSION_POPUP,
    UNKNOWN_SOURCES_POPUP,
    DOWNLOADING,
    READY_TO_INSTALL
}
