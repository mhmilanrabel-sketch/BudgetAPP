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

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey()
        }

        val secretKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            ?: throw IllegalStateException("Database key missing from Android Keystore")

        return secretKey.encoded
    }

    private fun generateKey() {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()

            Log.i(TAG, "Generated new hardware-backed AES-256 database key.")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to generate database key", t)
            throw IllegalStateException("Could not create database encryption key", t)
        }
    }
}
