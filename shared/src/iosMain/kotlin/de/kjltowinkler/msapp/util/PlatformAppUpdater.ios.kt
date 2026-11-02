package de.kjltowinkler.msapp.util

import de.kjltowinkler.msapp.data.update.AppUpdateInfo

actual object PlatformAppUpdater {
    actual val currentVersionCode: Int = 1
    actual val currentVersionName: String = "1.0.0"
    actual val isUpdateSupported: Boolean = false

    actual fun canInstallUnknownApps(): Boolean = true
    actual fun openUnknownAppSourcesSettings() {}

    actual suspend fun downloadAndInstall(
        updateInfo: AppUpdateInfo,
        onProgress: (progressPercent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<String> = Result.failure(UnsupportedOperationException("In-App Auto-Update ist auf iOS nicht verfügbar."))

    actual fun installApk(localPath: String): Result<Unit> = Result.failure(UnsupportedOperationException("APK-Installation nur auf Android."))
}
