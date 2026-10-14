package de.kjltowinkler.msapp.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

actual suspend fun shareImageBitmap(
    imageBitmap: ImageBitmap,
    title: String,
    filenamePrefix: String
) {
    val context = AppContextProvider.context
    android.util.Log.d("ShareImage", "shareImageBitmap gestartet. Bitmap-Größe: ${imageBitmap.width}x${imageBitmap.height}")
    withContext(Dispatchers.IO) {
        try {
            val bitmap = imageBitmap.asAndroidBitmap()

            // Cache-Verzeichnis für freigegebene Bilder erstellen
            val cacheFolder = File(context.cacheDir, "images").apply { mkdirs() }
            val imageFile = File(cacheFolder, "${filenamePrefix}_${System.currentTimeMillis()}.png")

            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            android.util.Log.d("ShareImage", "Bild erfolgreich gespeichert: ${imageFile.absolutePath} (Länge: ${imageFile.length()} Bytes)")

            // Sichere URI über FileProvider generieren
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
            android.util.Log.d("ShareImage", "FileProvider URI: $contentUri")

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)
            android.util.Log.d("ShareImage", "Chooser Activity erfolgreich gestartet")
        } catch (e: Throwable) {
            android.util.Log.e("ShareImage", "Fehler beim Teilen des Bildes", e)
            throw e
        }
    }
}
