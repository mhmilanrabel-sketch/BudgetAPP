package com.example.moneymanager.core.security

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Manages the SQLCipher database passphrase.
 *
 * On modern Android (API 28+), Keystore keys are non-exportable — `getEncoded()`
 * returns null. So we use a different pattern: generate a random 32-byte
 * passphrase once, encrypt it with a Keystore-backed MasterKey via
 * EncryptedSharedPreferences, and retrieve it whenever SQLCipher needs it.
 */
object KeyManager {

    private const val TAG = "KeyManager"
    private const val PREFS_NAME = "money_manager_secure_prefs"
    private const val KEY_PASSPHRASE = "db_passphrase"

    fun getOrCreateDatabasePassphrase(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val existing = prefs.getString(KEY_PASSPHRASE, null)
        if (existing != null) {
            return existing.toByteArray(Charsets.UTF_8)
        }

        // First run: generate a fresh 32-byte random passphrase.
        val random = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val passphrase = Base64.encodeToString(random, Base64.NO_WRAP)
        prefs.edit().putString(KEY_PASSPHRASE, passphrase).apply()
        Log.i(TAG, "Generated and stored new DB passphrase.")
        return passphrase.toByteArray(Charsets.UTF_8)
    }
}