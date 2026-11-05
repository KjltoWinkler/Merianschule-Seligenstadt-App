package de.kjltowinkler.msapp.server

import de.kjltowinkler.msapp.server.util.ApkManifestParser
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ApkManifestParserTest {

    @Test
    fun testParseLatestApk() {
        val apkFile = File("data/updates/latest.apk")
        if (!apkFile.exists()) {
            println("Test skipped: data/updates/latest.apk does not exist")
            return
        }

        val info = ApkManifestParser.parse(apkFile)
        assertNotNull(info)
        assertEquals("de.kjltowinkler.msapp", info.packageName)
        assertEquals(3, info.versionCode)
        assertEquals("1.0.2", info.versionName)
    }
}
