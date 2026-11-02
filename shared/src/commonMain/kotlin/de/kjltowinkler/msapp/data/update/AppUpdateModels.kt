package de.kjltowinkler.msapp.data.update

import kotlinx.serialization.Serializable

@Serializable
data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String = "",
    val fileSizeBytes: Long = 0L,
    val publishedAt: Long = 0L
)

sealed interface UpdateCheckStatus {
    data object Idle : UpdateCheckStatus
    data object Checking : UpdateCheckStatus
    data class UpdateAvailable(val updateInfo: AppUpdateInfo) : UpdateCheckStatus
    data object UpToDate : UpdateCheckStatus
    data class Error(val message: String) : UpdateCheckStatus
}

sealed interface UpdateDownloadStatus {
    data object Idle : UpdateDownloadStatus
    data class Downloading(val progressPercent: Int, val bytesDownloaded: Long, val totalBytes: Long) : UpdateDownloadStatus
    data class ReadyToInstall(val updateInfo: AppUpdateInfo, val localPath: String) : UpdateDownloadStatus
    data class Error(val message: String) : UpdateDownloadStatus
}
