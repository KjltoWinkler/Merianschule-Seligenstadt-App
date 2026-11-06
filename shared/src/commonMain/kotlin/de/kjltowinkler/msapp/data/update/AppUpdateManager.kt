package de.kjltowinkler.msapp.data.update

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.util.PlatformAppUpdater
import de.kjltowinkler.msapp.util.PlatformNetworkMonitor
import de.kjltowinkler.msapp.ui.components.AppSnackbarManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AppUpdateManager {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _checkStatus = mutableStateOf<UpdateCheckStatus>(UpdateCheckStatus.Idle)
    val checkStatus: State<UpdateCheckStatus> = _checkStatus

    private val _downloadStatus = mutableStateOf<UpdateDownloadStatus>(UpdateDownloadStatus.Idle)
    val downloadStatus: State<UpdateDownloadStatus> = _downloadStatus

    fun checkForUpdates(onComplete: ((UpdateCheckStatus) -> Unit)? = null) {
        if (!PlatformNetworkMonitor.isOnline.value) {
            AppSnackbarManager.showOfflineActionMessage("Update-Prüfung")
        }
        _checkStatus.value = UpdateCheckStatus.Checking
        scope.launch {
            val result = BackendApiClient.fetchAppUpdateInfo()
            result.onSuccess { info ->
                val currentCode = PlatformAppUpdater.currentVersionCode
                val status = if (info.versionCode > currentCode) {
                    UpdateCheckStatus.UpdateAvailable(info)
                } else {
                    UpdateCheckStatus.UpToDate
                }
                _checkStatus.value = status
                onComplete?.invoke(status)
            }.onFailure { err ->
                val status = UpdateCheckStatus.Error(err.message ?: "Verbindung zum Server fehlgeschlagen.")
                _checkStatus.value = status
                onComplete?.invoke(status)
            }
        }
    }

    /**
     * Prüft, ob ein neues Update verfügbar ist, und sendet bei Bedarf eine Push-Benachrichtigung.
     * @param forceNotify Wenn true, wird auch benachrichtigt, wenn für diese Version bereits benachrichtigt wurde.
     */
    suspend fun checkAndNotifyUpdate(forceNotify: Boolean = false): Boolean {
        return try {
            val result = BackendApiClient.fetchAppUpdateInfo()
            val info = result.getOrNull() ?: return false
            val currentCode = PlatformAppUpdater.currentVersionCode
            if (info.versionCode > currentCode) {
                _checkStatus.value = UpdateCheckStatus.UpdateAvailable(info)

                val lastNotified = de.kjltowinkler.msapp.data.storage.PlatformStorage.getString("last_notified_update_version_code")?.toIntOrNull() ?: 0
                val lastNotifiedTime = de.kjltowinkler.msapp.data.storage.PlatformStorage.getString("last_notified_update_time")?.toLongOrNull() ?: 0L
                val now = de.kjltowinkler.msapp.data.task.getCurrentEpochMillis()
                val cooldownPassed = (now - lastNotifiedTime) > (12 * 60 * 60 * 1000L) // Erinnerung frühestens alle 12 Stunden

                val shouldNotify = forceNotify || (info.versionCode > lastNotified) || (info.versionCode == lastNotified && cooldownPassed)

                if (shouldNotify) {
                    val title = "Neues App-Update verfügbar! 🚀"
                    val summary = "Version ${info.versionName} steht zum Download bereit."
                    val details = mutableListOf<String>().apply {
                        add("Version ${info.versionName} (Build ${info.versionCode})")
                        if (info.releaseNotes.isNotBlank()) {
                            add("Änderungen: ${info.releaseNotes}")
                        }
                        add("Tippe hier zum Herunterladen & Installieren.")
                    }
                    val sent = de.kjltowinkler.msapp.util.PlatformNotifier.sendUpdateNotification(title, summary, details)
                    if (sent) {
                        de.kjltowinkler.msapp.data.storage.PlatformStorage.setString("last_notified_update_version_code", info.versionCode.toString())
                        de.kjltowinkler.msapp.data.storage.PlatformStorage.setString("last_notified_update_time", now.toString())
                        true
                    } else {
                        // Noch nicht zugestellt (z. B. Berechtigung noch nicht erteilt) -> nicht als zugestellt markieren!
                        false
                    }
                } else {
                    false
                }
            } else {
                _checkStatus.value = UpdateCheckStatus.UpToDate
                false
            }
        } catch (e: Throwable) {
            println("AppUpdateManager: Fehler beim Update-Check: ${e.message}")
            false
        }
    }

    fun startDownloadAndInstall(updateInfo: AppUpdateInfo) {
        _downloadStatus.value = UpdateDownloadStatus.Downloading(0, 0L, updateInfo.fileSizeBytes)
        scope.launch {
            val res = PlatformAppUpdater.downloadAndInstall(updateInfo) { percent, downloaded, total ->
                _downloadStatus.value = UpdateDownloadStatus.Downloading(percent, downloaded, total)
            }
            res.onSuccess { localPath ->
                _downloadStatus.value = UpdateDownloadStatus.ReadyToInstall(updateInfo, localPath)
            }.onFailure { err ->
                _downloadStatus.value = UpdateDownloadStatus.Error(err.message ?: "Download fehlgeschlagen.")
            }
        }
    }

    fun onApkInstallationLaunched() {
        _downloadStatus.value = UpdateDownloadStatus.Idle
        checkForUpdates()
    }

    fun installApk(localPath: String): Result<Unit> {
        val res = PlatformAppUpdater.installApk(localPath)
        if (res.isSuccess) {
            onApkInstallationLaunched()
        }
        return res
    }

    fun resetStatus() {
        _checkStatus.value = UpdateCheckStatus.Idle
        _downloadStatus.value = UpdateDownloadStatus.Idle
    }
}
