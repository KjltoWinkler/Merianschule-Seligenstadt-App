package de.kjltowinkler.msapp.util

import androidx.compose.ui.graphics.ImageBitmap

actual suspend fun shareImageBitmap(
    imageBitmap: ImageBitmap,
    title: String,
    filenamePrefix: String
) {
    // Auf Desktop ist Teilen optional bzw. kann später mit Desktop Clipboard oder File Dialog erweitert werden
}
