package de.kjltowinkler.msapp.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
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
    val srcImage = ImageIO.read(ByteArrayInputStream(sourceBytes))
        ?: error("Bild konnte auf Desktop nicht gelesen werden")

    val rotated = if (rotationDegrees % 360 != 0) {
        val rads = Math.toRadians(rotationDegrees.toDouble())
        val sin = Math.abs(Math.sin(rads))
        val cos = Math.abs(Math.cos(rads))
        val w = srcImage.width
        val h = srcImage.height
        val newW = Math.floor(w * cos + h * sin).toInt()
        val newH = Math.floor(h * cos + w * sin).toInt()

        val rotImg = BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB)
        val g2d = rotImg.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        val at = AffineTransform()
        at.translate((newW - w) / 2.0, (newH - h) / 2.0)
        at.rotate(rads, w / 2.0, h / 2.0)
        g2d.drawRenderedImage(srcImage, at)
        g2d.dispose()
        rotImg
    } else {
        srcImage
    }

    val imgW = rotated.width
    val imgH = rotated.height

    var x = (cropRect.left * imgW).roundToInt()
    var y = (cropRect.top * imgH).roundToInt()
    var w = (cropRect.width * imgW).roundToInt()
    var h = (cropRect.height * imgH).roundToInt()

    x = max(0, min(x, imgW - 1))
    y = max(0, min(y, imgH - 1))
    w = max(1, min(w, imgW - x))
    h = max(1, min(h, imgH - y))

    val cropped = rotated.getSubimage(x, y, w, h)

    val finalImg = BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_RGB)
    val g = finalImg.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    g.drawImage(cropped, 0, 0, targetSize, targetSize, null)
    g.dispose()

    val out = ByteArrayOutputStream()
    ImageIO.write(finalImg, "jpeg", out)
    out.toByteArray()
}
