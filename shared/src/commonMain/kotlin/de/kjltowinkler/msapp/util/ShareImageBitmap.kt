package de.kjltowinkler.msapp.util

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Platform-spezifisches Teilen einer gerenderten ImageBitmap als PNG über das System-Share-Sheet.
 */
expect suspend fun shareImageBitmap(
    imageBitmap: ImageBitmap,
    title: String = "Vertretungsplan teilen",
    filenamePrefix: String = "vertretungsplan"
)
