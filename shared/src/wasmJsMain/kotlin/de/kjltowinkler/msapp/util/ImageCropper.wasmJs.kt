package de.kjltowinkler.msapp.util

actual suspend fun cropAndCompressImage(
    sourceBytes: ByteArray,
    cropRect: NormalizedCropRect,
    rotationDegrees: Int,
    targetSize: Int,
    quality: Int
): ByteArray {
    // WasmJs Fallback: Gibt die Byte-Daten zurück
    return sourceBytes
}
