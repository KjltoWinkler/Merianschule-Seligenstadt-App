package de.kjltowinkler.msapp.util

import androidx.compose.runtime.Composable

@Composable
actual fun rememberPlatformImagePicker(
    onImagePicked: (bytes: ByteArray, mimeType: String) -> Unit
): () -> Unit {
    return {
        // iOS Photo Picker Stub
    }
}
