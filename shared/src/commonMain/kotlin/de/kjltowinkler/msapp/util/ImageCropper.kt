package de.kjltowinkler.msapp.util

data class NormalizedCropRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
)

/**
 * Schneidet ein Quellbild auf einen normalisierten rechteckigen Bereich zu,
 * wendet optionale Rotation an, skaliert auf die gewünschte Zielgröße (z. B. 512x512)
 * und komprimiert das Ergebnis als JPEG-ByteArray.
 */
expect suspend fun cropAndCompressImage(
    sourceBytes: ByteArray,
    cropRect: NormalizedCropRect,
    rotationDegrees: Int = 0,
    targetSize: Int = 512,
    quality: Int = 90
): ByteArray
