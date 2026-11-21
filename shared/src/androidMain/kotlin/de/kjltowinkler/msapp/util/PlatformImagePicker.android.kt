package de.kjltowinkler.msapp.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberPlatformImagePicker(
    onImagePicked: (bytes: ByteArray, mimeType: String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val cr = context.contentResolver

                    // 1. EXIF-Orientierung ermitteln
                    val orientation = try {
                        cr.openInputStream(uri)?.use { stream ->
                            val exif = ExifInterface(stream)
                            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                        } ?: ExifInterface.ORIENTATION_NORMAL
                    } catch (_: Throwable) {
                        ExifInterface.ORIENTATION_NORMAL
                    }

                    // 2. Abmessungen ohne Bitmap-Allokation lesen
                    val boundsOptions = BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    cr.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, boundsOptions)
                    }

                    // 3. Sicheres inSampleSize berechnen (maximal 2048 Pixel Kantenlänge)
                    val maxDim = 2048
                    var sampleSize = 1
                    while (boundsOptions.outWidth / sampleSize > maxDim || boundsOptions.outHeight / sampleSize > maxDim) {
                        sampleSize *= 2
                    }

                    // 4. Speicher-sicheres Dekodieren
                    val decodeOptions = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val rawBitmap = cr.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, decodeOptions)
                    } ?: return@launch

                    // 5. EXIF-Drehung korrigieren
                    val rotationDegrees = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                    val orientedBitmap = if (rotationDegrees != 0f) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees) }
                        val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                        if (rotated != rawBitmap) {
                            rawBitmap.recycle()
                        }
                        rotated
                    } else {
                        rawBitmap
                    }

                    // 6. Als hochqualitatives, speicherschonendes JPEG komprimieren
                    val out = ByteArrayOutputStream()
                    orientedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    orientedBitmap.recycle()
                    val safeBytes = out.toByteArray()

                    if (safeBytes.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            onImagePicked(safeBytes, "image/jpeg")
                        }
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }
    }

    return {
        launcher.launch("image/*")
    }
}
