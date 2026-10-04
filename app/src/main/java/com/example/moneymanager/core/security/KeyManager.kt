package com.example.moneymanager.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed key manager utilizing Android KeyStore to generate and protect
 * a 256-bit passphrase for SQLCipher database encryption.
 *
 * Never writes raw passphrases to disk unencrypted.
 */
object KeyManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "MoneyManagerMasterKey"
    private const val ENCRYPTED_KEY_FILE = "db_pass.enc"
    private const val IV_FILE = "db_pass.iv"
    private const val AES_GCM_NOPADDING = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BITS = 128
    private const val KEY_SIZE_BYTES = 32 // 256-bit key

    private var cachedPassphrase: ByteArray? = null

    @Synchronized
    fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
        cachedPassphrase?.let { return it }

        val filesDir = context.filesDir
        val encFile = File(filesDir, ENCRYPTED_KEY_FILE)
        val ivFile = File(filesDir, IV_FILE)

        return if (encFile.exists() && ivFile.exists()) {
            try {
                val encryptedBytes = encFile.readBytes()
                val iv = ivFile.readBytes()
                val secretKey = getMasterKey()
                val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
                val decrypted = cipher.doFinal(encryptedBytes)
                cachedPassphrase = decrypted
                decrypted
            } catch (e: Exception) {
                // If corrupted or tampered, regenerate key
                generateAndSaveNewPassphrase(encFile, ivFile)
            }
        } else {
            generateAndSaveNewPassphrase(encFile, ivFile)
        }
    }

    private fun generateAndSaveNewPassphrase(encFile: File, ivFile: File): ByteArray {
        val randomBytes = ByteArray(KEY_SIZE_BYTES)
        SecureRandom().nextBytes(randomBytes)

        val secretKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance(AES_GCM_NOPADDING)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(randomBytes)

        encFile.writeBytes(encrypted)
        ivFile.writeBytes(iv)

        cachedPassphrase = randomBytes
        return randomBytes
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun getMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            return getOrCreateMasterKey()
        }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Helper to clear cached sensitive key material from memory when app is locked
     */
    @Synchronized
    fun clearCache() {
        cachedPassphrase?.fill(0)
        cachedPassphrase = null
    }
}
