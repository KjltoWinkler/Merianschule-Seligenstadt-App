package de.kjltowinkler.msapp.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import de.kjltowinkler.msapp.data.backend.BackendApiClient
import de.kjltowinkler.msapp.data.update.AppUpdateInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

actual object PlatformAppUpdater {

    actual val currentVersionCode: Int
        get() = try {
            val context = AppContextProvider.context
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            PackageInfoCompat.getLongVersionCode(pInfo).toInt()
        } catch (_: Throwable) {
            1
        }

    actual val currentVersionName: String
        get() = try {
            val context = AppContextProvider.context
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "1.0"
        } catch (_: Throwable) {
            "1.0"
        }

    actual val isUpdateSupported: Boolean = true

    actual fun canInstallUnknownApps(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                AppContextProvider.context.packageManager.canRequestPackageInstalls()
            } catch (_: Throwable) {
                true
            }
        } else {
            true
        }
    }

    actual fun openUnknownAppSourcesSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val context = AppContextProvider.context
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Throwable) {
                // Fallback zu allgemeinen Sicherheitseinstellungen
                try {
                    val fallbackIntent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    AppContextProvider.context.startActivity(fallbackIntent)
                } catch (_: Throwable) {}
            }
        }
    }

    actual suspend fun downloadAndInstall(
        updateInfo: AppUpdateInfo,
        onProgress: (progressPercent: Int, bytesDownloaded: Long, totalBytes: Long) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val context = AppContextProvider.context
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val cleanVersionName = updateInfo.versionName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
            val destinationFile = File(updatesDir, "MerianschuleApp-$cleanVersionName.apk")
            if (destinationFile.exists()) destinationFile.delete()

            val rawUrl = if (updateInfo.downloadUrl.startsWith("http://") || updateInfo.downloadUrl.startsWith("https://")) {
                updateInfo.downloadUrl
            } else {
                "${BackendApiClient.serverUrl.trimEnd('/')}/${updateInfo.downloadUrl.trimStart('/')}"
            }
            val separator = if (rawUrl.contains("?")) "&" else "?"
            val fullUrl = "$rawUrl${separator}t=${System.currentTimeMillis()}"

            val client = HttpClient(CIO)
            val response = client.get(fullUrl)
            if (!response.status.isSuccess()) {
                throw IllegalStateException("Download fehlgeschlagen (HTTP ${response.status.value})")
            }

            val totalBytes = response.contentLength() ?: updateInfo.fileSizeBytes
            val channel = response.bodyAsChannel()
            val inputStream = channel.toInputStream()
            var downloadedBytes = 0L

            FileOutputStream(destinationFile).use { fos ->
                val buffer = ByteArray(32 * 1024)
                var read = inputStream.read(buffer)
                while (read >= 0) {
                    fos.write(buffer, 0, read)
                    downloadedBytes += read
                    val percent = if (totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0
                    onProgress(percent, downloadedBytes, totalBytes)
                    read = inputStream.read(buffer)
                }
                fos.flush()
            }

            onProgress(100, downloadedBytes, if (totalBytes > 0) totalBytes else downloadedBytes)

            // Installieren direkt anstoßen
            installApk(destinationFile.absolutePath).getOrThrow()

            destinationFile.absolutePath
        }
    }

    actual fun installApk(localPath: String): Result<Unit> = runCatching {
        val context = AppContextProvider.context
        val file = File(localPath)
        if (!file.exists()) {
            throw IllegalArgumentException("APK-Datei existiert nicht: $localPath")
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
