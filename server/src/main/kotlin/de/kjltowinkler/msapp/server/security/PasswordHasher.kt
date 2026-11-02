package de.kjltowinkler.msapp.server.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private val secureRandom = SecureRandom()

    fun generateSalt(): String {
        val salt = ByteArray(16)
        secureRandom.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    fun generateInitialPassword(length: Int = 10): String {
        val uppercase = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        val lowercase = "abcdefghjkmnpqrstuvwxyz"
        val digits = "23456789"
        val allChars = uppercase + lowercase + digits

        val chars = CharArray(length)
        chars[0] = uppercase[secureRandom.nextInt(uppercase.length)]
        chars[1] = lowercase[secureRandom.nextInt(lowercase.length)]
        chars[2] = digits[secureRandom.nextInt(digits.length)]
        for (i in 3 until length) {
            chars[i] = allChars[secureRandom.nextInt(allChars.length)]
        }
        for (i in chars.indices.reversed()) {
            val j = secureRandom.nextInt(i + 1)
            val temp = chars[i]
            chars[i] = chars[j]
            chars[j] = temp
        }
        return String(chars)
    }

    fun hashPassword(password: String, saltBase64: String): String {
        val salt = Base64.getDecoder().decode(saltBase64)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hash = factory.generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(hash)
    }

    fun verifyPassword(password: String, saltBase64: String, expectedHashBase64: String): Boolean {
        val actualHash = hashPassword(password, saltBase64)
        val actualBytes = Base64.getDecoder().decode(actualHash)
        val expectedBytes = Base64.getDecoder().decode(expectedHashBase64)
        return MessageDigest.isEqual(actualBytes, expectedBytes)
    }
}
