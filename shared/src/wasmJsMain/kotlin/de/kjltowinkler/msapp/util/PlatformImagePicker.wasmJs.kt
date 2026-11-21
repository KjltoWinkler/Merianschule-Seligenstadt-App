package de.kjltowinkler.msapp.util

import androidx.compose.runtime.Composable
import kotlinx.browser.document
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get

@Composable
actual fun rememberPlatformImagePicker(
    onImagePicked: (bytes: ByteArray, mimeType: String) -> Unit
): () -> Unit {
    return {
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = "image/png,image/jpeg,image/webp"
        input.onchange = {
            val file = input.files?.get(0)
            if (file != null) {
                val reader = FileReader()
                val mimeType = if (file.type.isNotBlank()) file.type else "image/jpeg"
                reader.onload = {
                    val buffer = reader.result as? ArrayBuffer
                    if (buffer != null) {
                        val int8Array = Int8Array(buffer)
                        val bytes = ByteArray(int8Array.length) { i -> int8Array[i] }
                        onImagePicked(bytes, mimeType)
                    }
                }
                reader.readAsArrayBuffer(file)
            }
        }
        input.click()
    }
}
