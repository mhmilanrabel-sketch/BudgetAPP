package com.example.moneymanager.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Manages the hardware-backed AES-256 key used by SQLCipher to encrypt the Room DB.
 * The raw key never leaves the Android Keystore; only a byte copy is returned when needed.
 */
object KeyManager {

    private const val TAG = "KeyManager"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "moneymanager_lk_db_key"

    fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) generateKey()
        val secretKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            ?: throw IllegalStateException("DB key missing from Keystore")
        return secretKey.encoded
    }

    private fun generateKey() {
        try {
            val kg = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            kg.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setUserAuthenticationRequired(false)
                    .build()
            )
            kg.generateKey()
            Log.i(TAG, "Generated AES-256 DB key.")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to generate DB key", t)
            throw IllegalStateException("Could not create DB encryption key", t)
        }
    }
}