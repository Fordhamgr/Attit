package com.example.attit.utils

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Moderate AES Encryption for Firebase Data
 * Provides an obfuscation layer so data is not plain-text in the cloud.
 * The decrypt wrapper falls back to returning the plain-text if it fails
 * (crucial for backwards compatibility with already-existing non-encrypted data).
 */
object CryptoUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    // 16 bytes for AES-128
    private val KEY = "AttitSecureKey12".toByteArray(Charsets.UTF_8)
    private val IV = "AttitSecureIV123".toByteArray(Charsets.UTF_8)

    fun encrypt(input: String): String {
        return try {
            if (input.isEmpty()) return input
            val cipher = Cipher.getInstance(ALGORITHM)
            val keySpec = SecretKeySpec(KEY, "AES")
            val ivSpec = IvParameterSpec(IV)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            val encryptedBytes = cipher.doFinal(input.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            input // Fallback safely
        }
    }

    fun decrypt(input: String): String {
        return try {
            if (input.isEmpty()) return input
            val cipher = Cipher.getInstance(ALGORITHM)
            val keySpec = SecretKeySpec(KEY, "AES")
            val ivSpec = IvParameterSpec(IV)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            val decodedBytes = Base64.decode(input, Base64.NO_WRAP)
            String(cipher.doFinal(decodedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            // Backward Compatibility: If the string is not Base64 or not encrypted with this key,
            // (meaning it's legacy plain-text data), just return the plain-text string!
            input
        }
    }
}
