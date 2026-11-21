package de.kjltowinkler.msapp.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

actual suspend fun cropAndCompressImage(
    sourceBytes: ByteArray,
    cropRect: NormalizedCropRect,
    rotationDegrees: Int,
    targetSize: Int,
    quality: Int
): ByteArray = withContext(Dispatchers.Default) {
    val srcBitmap = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size)
        ?: error("Bild konnte auf Android nicht dekodiert werden")

    val rotatedBitmap = if (rotationDegrees % 360 != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.width, srcBitmap.height, matrix, true)
        if (rotated != srcBitmap) {
            srcBitmap.recycle()
        }
        rotated
    } else {
        srcBitmap
    }

    val imgW = rotatedBitmap.width
    val imgH = rotatedBitmap.height

    var x = (cropRect.left * imgW).roundToInt()
    var y = (cropRect.top * imgH).roundToInt()
    var w = (cropRect.width * imgW).roundToInt()
    var h = (cropRect.height * imgH).roundToInt()

    x = max(0, min(x, imgW - 1))
    y = max(0, min(y, imgH - 1))
    w = max(1, min(w, imgW - x))
    h = max(1, min(h, imgH - y))

    val cropped = Bitmap.createBitmap(rotatedBitmap, x, y, w, h)
    if (cropped != rotatedBitmap) {
        rotatedBitmap.recycle()
    }

    val scaled = if (cropped.width != targetSize || cropped.height != targetSize) {
        val s = Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)
        if (s != cropped) {
            cropped.recycle()
        }
        s
    } else {
        cropped
    }

    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
    scaled.recycle()
    out.toByteArray()
}
