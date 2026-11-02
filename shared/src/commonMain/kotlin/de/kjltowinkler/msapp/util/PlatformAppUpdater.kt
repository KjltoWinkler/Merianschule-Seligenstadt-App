package de.kjltowinkler.msapp.util

import de.kjltowinkler.msapp.data.update.AppUpdateInfo

expect object PlatformAppUpdater {
    val currentVersionCode: Int
    val currentVersionName: String
    val isUpdateSupported: Boolean

    fun canInstallUnknownApps(): Boolean
    fun openUnknownAppSourcesSettings()

    suspend fun downloadAndInstall(
        updateInfo: AppUpdateInfo,
        onProgress: (progressPercent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<String>

    fun installApk(localPath: String): Result<Unit>
}
