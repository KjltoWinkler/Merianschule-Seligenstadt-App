package de.kjltowinkler.msapp.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

@Composable
actual fun rememberPlatformImagePicker(
    onImagePicked: (bytes: ByteArray, mimeType: String) -> Unit
): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch(Dispatchers.IO) {
            try {
                val dialog = FileDialog(null as Frame?, "Profilbild auswählen", FileDialog.LOAD)
                dialog.setFilenameFilter { _, name ->
                    val lower = name.lowercase()
                    lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp")
                }
                dialog.isVisible = true
                val dir = dialog.directory
                val file = dialog.file
                if (dir != null && file != null) {
                    val selectedFile = File(dir, file)
                    if (selectedFile.exists() && selectedFile.isFile) {
                        val bytes = selectedFile.readBytes()
                        val ext = selectedFile.extension.lowercase()
                        val mimeType = when (ext) {
                            "png" -> "image/png"
                            "webp" -> "image/webp"
                            else -> "image/jpeg"
                        }
                        withContext(Dispatchers.Main) {
                            onImagePicked(bytes, mimeType)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
