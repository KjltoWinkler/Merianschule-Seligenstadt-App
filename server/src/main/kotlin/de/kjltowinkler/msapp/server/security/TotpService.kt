package de.kjltowinkler.msapp.server.security

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Standardkonforme Implementierung von Time-based One-Time Password (TOTP)
 * gemäß RFC 6238 und HMAC-Based One-Time Password (HOTP) gemäß RFC 4226.
 *
 * Kompatibel mit allen gängigen Authenticator-Apps:
 * - Google Authenticator
 * - Microsoft Authenticator
 * - Apple Keychain (iOS Passwörter)
 * - 2FAS / Aegis / Bitwarden / 1Password
 */
object TotpService {

    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private const val TIME_STEP_SECONDS = 30L
    private const val CODE_DIGITS = 6
    private val secureRandom = SecureRandom()

    /**
     * Generiert ein kryptographisch sicheres Base32-Secret (160 Bit / 20 Bytes = 32 Base32-Zeichen).
     */
    fun generateSecret(byteLength: Int = 20): String {
        val bytes = ByteArray(byteLength)
        secureRandom.nextBytes(bytes)
        return encodeBase32(bytes)
    }

    /**
     * Erstellt eine standardkonforme otpauth:// URL für Authenticator-Apps.
     */
    fun generateOtpAuthUri(
        issuer: String = "Merianschule",
        accountName: String,
        secret: String
    ): String {
        val cleanSecret = secret.replace(" ", "").uppercase()
        val cleanAccount = accountName.trim().replace(":", "_").replace(" ", "%20")
        val cleanIssuer = issuer.trim().replace(":", "_").replace(" ", "%20")
        return "otpauth://totp/$cleanIssuer:$cleanAccount?secret=$cleanSecret&issuer=$cleanIssuer"
    }

    /**
     * Formatiert ein Base32-Secret in 4er-Blöcke (z.B. "ABCD EFGH 1234 ...") zur leichteren manuellen Eingabe.
     */
    fun formatSecret(secret: String): String {
        val clean = secret.replace(" ", "").uppercase()
        return clean.chunked(4).joinToString(" ")
    }

    /**
     * Verifiziert einen vom Nutzer eingegebenen 6-stelligen Code gegen das Secret.
     * Unterstützt ein Zeitfenster von ±2 Zeitschritten (±60 Sekunden),
     * um leichte Zeitabweichungen auf Endgeräten sicher auszugleichen.
     */
    fun verifyCode(
        secret: String,
        code: String,
        window: Int = 2,
        timeMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val cleanCode = code.filter { it.isDigit() }
        if (cleanCode.length != CODE_DIGITS) return false

        val cleanSecret = secret.replace(" ", "").uppercase()
        val keyBytes = decodeBase32(cleanSecret) ?: return false

        val currentStep = (timeMillis / 1000L) / TIME_STEP_SECONDS
        for (i in -window..window) {
            val candidate = generateCodeForStep(keyBytes, currentStep + i)
            if (candidate == cleanCode) {
                return true
            }
        }
        return false
    }

    /**
     * Erzeugt den 6-stelligen TOTP-Code für einen gegebenen Zeitschritt.
     */
    fun generateCodeForStep(keyBytes: ByteArray, timeStep: Long): String {
        val data = ByteBuffer.allocate(8).putLong(timeStep).array()
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA1"))
        val hash = mac.doFinal(data)

        val offset = (hash[hash.size - 1].toInt() and 0x0F)
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val otp = binary % 1_000_000
        return otp.toString().padStart(CODE_DIGITS, '0')
    }

    /**
     * Berechnet den aktuellen Code (nützlich für automatisierte Tests).
     */
    fun generateCurrentCode(secret: String, timeMillis: Long = System.currentTimeMillis()): String? {
        val cleanSecret = secret.replace(" ", "").uppercase()
        val keyBytes = decodeBase32(cleanSecret) ?: return null
        val currentStep = (timeMillis / 1000L) / TIME_STEP_SECONDS
        return generateCodeForStep(keyBytes, currentStep)
    }

    fun encodeBase32(bytes: ByteArray): String {
        val sb = StringBuilder((bytes.size * 8 + 4) / 5)
        var buffer = 0
        var bitsLeft = 0
        for (b in bytes) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                val index = (buffer shr (bitsLeft - 5)) and 0x1F
                sb.append(BASE32_ALPHABET[index])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            val index = (buffer shl (5 - bitsLeft)) and 0x1F
            sb.append(BASE32_ALPHABET[index])
        }
        return sb.toString()
    }

    fun decodeBase32(base32: String): ByteArray? {
        val clean = base32.trim().replace("=", "").replace(" ", "").uppercase()
        if (clean.isEmpty()) return null

        val bytes = mutableListOf<Byte>()
        var buffer = 0
        var bitsLeft = 0
        for (c in clean) {
            val index = BASE32_ALPHABET.indexOf(c)
            if (index < 0) return null
            buffer = (buffer shl 5) or index
            bitsLeft += 5
            while (bitsLeft >= 8) {
                bytes.add(((buffer shr (bitsLeft - 8)) and 0xFF).toByte())
                bitsLeft -= 8
            }
        }
        return bytes.toByteArray()
    }
}
