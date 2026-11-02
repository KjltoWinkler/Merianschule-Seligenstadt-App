package de.kjltowinkler.msapp.server.security

import de.kjltowinkler.msapp.server.config.ServerConfig
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Kryptographischer Verschlüsselungsdienst zur sicheren, DSGVO-konformen Speicherung
 * von Schüleraufgaben und sensiblen Nutzerdaten (z. B. 2FA-Geheimnisse) in der Datenbank.
 * Verwendet authentifizierte Verschlüsselung: AES-256-GCM mit eindeutigem 96-Bit-IV pro Vorgang.
 */
object DataEncryptionService {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12
    private const val PREFIX = "enc:v1:"

    private val secureRandom = SecureRandom()

    // 256-Bit AES Masterschlüssel, abgeleitet über PBKDF2WithHmacSHA256
    private val masterKey: SecretKeySpec by lazy {
        val secret = ServerConfig.dataEncryptionKey
        val salt = "merianschule-dsgvo-task-salt-v1".toByteArray(Charsets.UTF_8)
        val spec = PBEKeySpec(secret.toCharArray(), salt, 65536, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Verschlüsselt einen Klartext mit AES-256-GCM.
     * Erzeugt für jede Verschlüsselung einen kryptographisch sicheren, zufälligen 12-Byte-IV.
     * Gibt einen String im Format "enc:v1:<Base64(IV + Ciphertext + Tag)>" zurück.
     */
    fun encrypt(plaintext: String?): String {
        if (plaintext.isNullOrEmpty()) return ""
        // Bereits verschlüsselte Texte nicht doppelt verschlüsseln
        if (plaintext.startsWith(PREFIX)) return plaintext

        return try {
            val iv = ByteArray(IV_LENGTH_BYTES)
            secureRandom.nextBytes(iv)

            val cipher = Cipher.getInstance(ALGORITHM)
            val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, parameterSpec)

            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

            PREFIX + Base64.getEncoder().encodeToString(combined)
        } catch (e: Exception) {
            println("Warnung bei Datenverschlüsselung: ${e.message}")
            plaintext
        }
    }

    /**
     * Entschlüsselt einen mit AES-256-GCM verschlüsselten String.
     * Falls der String unverschlüsselt vorliegt (z. B. bestehende Altdaten),
     * wird er unverändert zurückgegeben (vollständige Abwärtskompatibilität).
     */
    fun decrypt(ciphertext: String?): String {
        if (ciphertext.isNullOrEmpty()) return ""
        if (!ciphertext.startsWith(PREFIX)) return ciphertext // Altdaten / Fallback

        return try {
            val rawB64 = ciphertext.removePrefix(PREFIX)
            val combined = Base64.getDecoder().decode(rawB64)
            if (combined.size < IV_LENGTH_BYTES + (TAG_LENGTH_BITS / 8)) {
                return ciphertext
            }

            val iv = ByteArray(IV_LENGTH_BYTES)
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES)

            val encryptedBytes = ByteArray(combined.size - IV_LENGTH_BYTES)
            System.arraycopy(combined, IV_LENGTH_BYTES, encryptedBytes, 0, encryptedBytes.size)

            val cipher = Cipher.getInstance(ALGORITHM)
            val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, masterKey, parameterSpec)

            val decrypted = cipher.doFinal(encryptedBytes)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            println("Warnung bei Datenentschlüsselung: ${e.message}")
            ciphertext
        }
    }
}
