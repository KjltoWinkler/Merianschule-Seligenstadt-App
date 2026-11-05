package de.kjltowinkler.msapp.server.util

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

data class ApkManifestInfo(
    val packageName: String,
    val versionCode: Int,
    val versionName: String
)

object ApkManifestParser {

    /**
     * Extrahiert Paketname, Versionscode und Versionsname direkt aus dem binären AndroidManifest.xml einer APK.
     */
    fun parse(apkFile: File): ApkManifestInfo? {
        if (!apkFile.exists() || !apkFile.isFile) return null

        return try {
            ZipFile(apkFile).use { zip ->
                val entry = zip.getEntry("AndroidManifest.xml") ?: return null
                val bytes = zip.getInputStream(entry).use { it.readBytes() }
                parseManifestBytes(bytes)
            }
        } catch (e: Throwable) {
            println("ApkManifestParser: Fehler beim Parsen von ${apkFile.name}: ${e.message}")
            null
        }
    }

    private fun parseManifestBytes(data: ByteArray): ApkManifestInfo? {
        if (data.size < 8) return null
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        val rootType = buffer.short.toInt() and 0xFFFF
        val rootHeaderSize = buffer.short.toInt() and 0xFFFF
        val rootFileSize = buffer.int

        if (rootType != 0x0003) { // CHUNK_AXML_FILE
            return null
        }

        var pos = rootHeaderSize
        var stringPool: List<String> = emptyList()

        var parsedPackage: String? = null
        var parsedVersionCode: Int? = null
        var parsedVersionName: String? = null

        while (pos < data.size && (parsedVersionCode == null || parsedVersionName == null || parsedPackage == null)) {
            buffer.position(pos)
            val chunkType = buffer.short.toInt() and 0xFFFF
            val chunkHeaderSize = buffer.short.toInt() and 0xFFFF
            val chunkSize = buffer.int

            if (chunkSize <= 0 || pos + chunkSize > data.size) break

            when (chunkType) {
                0x0001 -> { // CHUNK_STRING_POOL
                    stringPool = parseStringPool(buffer, pos, chunkHeaderSize, chunkSize)
                }
                0x0180 -> { // CHUNK_RESOURCE_MAP
                    // Überspringen
                }
                0x0102 -> { // CHUNK_START_TAG
                    val tagResult = parseStartTag(buffer, pos, stringPool)
                    if (tagResult != null) {
                        if (tagResult.packageName != null) parsedPackage = tagResult.packageName
                        if (tagResult.versionCode != null) parsedVersionCode = tagResult.versionCode
                        if (tagResult.versionName != null) parsedVersionName = tagResult.versionName
                    }
                }
            }

            pos += chunkSize
        }

        return if (parsedVersionCode != null) {
            ApkManifestInfo(
                packageName = parsedPackage ?: "de.kjltowinkler.msapp",
                versionCode = parsedVersionCode,
                versionName = parsedVersionName ?: parsedVersionCode.toString()
            )
        } else {
            null
        }
    }

    private fun parseStringPool(
        buffer: ByteBuffer,
        chunkStart: Int,
        headerSize: Int,
        chunkSize: Int
    ): List<String> {
        buffer.position(chunkStart + 8)
        val stringCount = buffer.int
        val styleCount = buffer.int
        val flags = buffer.int
        val stringsStart = buffer.int
        val stylesStart = buffer.int

        val isUtf8 = (flags and (1 shl 8)) != 0

        val offsets = IntArray(stringCount)
        buffer.position(chunkStart + 28)
        for (i in 0 until stringCount) {
            offsets[i] = buffer.int
        }

        val stringsBase = chunkStart + stringsStart
        val result = ArrayList<String>(stringCount)

        for (offset in offsets) {
            val stringPos = stringsBase + offset
            if (stringPos >= buffer.capacity()) {
                result.add("")
                continue
            }
            buffer.position(stringPos)

            if (isUtf8) {
                var u16len = buffer.get().toInt() and 0xFF
                if ((u16len and 0x80) != 0) {
                    u16len = ((u16len and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF)
                }
                var u8len = buffer.get().toInt() and 0xFF
                if ((u8len and 0x80) != 0) {
                    u8len = ((u8len and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF)
                }
                val strBytes = ByteArray(u8len)
                buffer.get(strBytes)
                result.add(String(strBytes, StandardCharsets.UTF_8))
            } else {
                var u16len = buffer.short.toInt() and 0xFFFF
                if ((u16len and 0x8000) != 0) {
                    u16len = ((u16len and 0x7FFF) shl 16) or (buffer.short.toInt() and 0xFFFF)
                }
                val byteCount = u16len * 2
                val strBytes = ByteArray(byteCount)
                buffer.get(strBytes)
                result.add(String(strBytes, StandardCharsets.UTF_16LE))
            }
        }

        return result
    }

    private data class TagParseResult(
        val packageName: String?,
        val versionCode: Int?,
        val versionName: String?
    )

    private fun parseStartTag(
        buffer: ByteBuffer,
        chunkStart: Int,
        stringPool: List<String>
    ): TagParseResult? {
        // ResXMLTree_node (8 byte header + 4 byte lineNumber + 4 byte comment)
        // ResXMLTree_attrExt:
        // uint32 ns (4)
        // uint32 name (4) -> tag name index
        // uint16 attrStart (2)
        // uint16 attrSize (2)
        // uint16 attrCount (2)
        // uint16 idIndex (2)
        // uint16 classIndex (2)
        // uint16 styleIndex (2)
        buffer.position(chunkStart + 16)
        val nsIdx = buffer.int
        val nameIdx = buffer.int
        val tagName = if (nameIdx in stringPool.indices) stringPool[nameIdx] else ""

        if (tagName != "manifest") {
            return null
        }

        val attrStart = buffer.short.toInt() and 0xFFFF
        val attrSize = buffer.short.toInt() and 0xFFFF
        val attrCount = buffer.short.toInt() and 0xFFFF

        var pkg: String? = null
        var vCode: Int? = null
        var vName: String? = null

        val attrOffset = chunkStart + 16 + attrStart

        for (i in 0 until attrCount) {
            val cur = attrOffset + i * attrSize
            if (cur + 20 > buffer.capacity()) break
            buffer.position(cur)

            val aNs = buffer.int
            val aNameIdx = buffer.int
            val aRawStrIdx = buffer.int
            val aSize = buffer.short.toInt() and 0xFFFF
            val aRes0 = buffer.get().toInt() and 0xFF
            val aDataType = buffer.get().toInt() and 0xFF
            val aData = buffer.int

            val attrName = if (aNameIdx in stringPool.indices) stringPool[aNameIdx] else ""

            when (attrName) {
                "versionCode" -> {
                    vCode = aData
                }
                "versionName" -> {
                    vName = if (aRawStrIdx != -1 && aRawStrIdx in stringPool.indices) {
                        stringPool[aRawStrIdx]
                    } else {
                        aData.toString()
                    }
                }
                "package" -> {
                    if (aRawStrIdx != -1 && aRawStrIdx in stringPool.indices) {
                        pkg = stringPool[aRawStrIdx]
                    }
                }
            }
        }

        return TagParseResult(pkg, vCode, vName)
    }
}
