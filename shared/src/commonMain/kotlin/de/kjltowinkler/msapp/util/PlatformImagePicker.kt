package de.kjltowinkler.msapp.util

import androidx.compose.runtime.Composable

/**
 * Universeller Multiplatform-Image-Picker für alle unterstützten Plattformen (Android, Desktop, Web, iOS).
 * Ermöglicht dem Nutzer, ein lokales Bild (JPEG, PNG, WebP) auszuwählen.
 * Liefert die Rohdaten (ByteArray) und den MIME-Typ des Bildes an den Callback zurück.
 */
@Composable
expect fun rememberPlatformImagePicker(
    onImagePicked: (bytes: ByteArray, mimeType: String) -> Unit
): () -> Unit
