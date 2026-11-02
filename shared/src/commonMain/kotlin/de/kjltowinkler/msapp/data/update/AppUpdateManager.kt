package de.kjltowinkler.msapp.data.update

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.util.PlatformAppUpdater
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

    fun installApk(localPath: String): Result<Unit> {
        return PlatformAppUpdater.installApk(localPath)
    }

    fun resetStatus() {
        _checkStatus.value = UpdateCheckStatus.Idle
        _downloadStatus.value = UpdateDownloadStatus.Idle
    }
}
