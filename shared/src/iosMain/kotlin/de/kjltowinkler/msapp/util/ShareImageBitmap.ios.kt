package de.kjltowinkler.msapp.util

import androidx.compose.ui.graphics.ImageBitmap

actual suspend fun shareImageBitmap(
    imageBitmap: ImageBitmap,
    title: String,
    filenamePrefix: String
) {
    // No-op bzw. iOS Activity View Controller Stub falls auf iOS ausgeführt
}
