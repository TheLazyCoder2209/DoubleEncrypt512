package com.lazycoder.doubleencrypt512

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class VaultStorage(private val context: Context) {
    private var sharedPreferences: SharedPreferences

    init {
        // Build the MasterKey - hardware-backed
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        // Initialize EncryptedSharedPreferences
        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "secure_vault_prefs_v2", // Updated name to avoid conflicts with broken v1 data
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * ****ROUTER LOGIC****
     * MainActivity checks this to decide if it should force the SetupWizard.
     */
    fun isInitialized(): Boolean {
        return sharedPreferences.getBoolean("setup_complete", false)
    }

    fun setDefaultDirectory(uri: Uri) {
        try {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            sharedPreferences.edit().putString("default_directory_uri", uri.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getDefaultDirectory(): Uri? {
        val uriString = sharedPreferences.getString("default_directory_uri", null)
        return uriString?.let { Uri.parse(it) }
    }

    fun saveTagKeys(tagId: String, keys: List<ByteArray>) {
        val editor = sharedPreferences.edit()
        keys.forEachIndexed { index, key ->
            val hexKey = key.joinToString("") { "%02x".format(it) }
            editor.putString("${tagId}_sector_$index", hexKey)
        }
        // This is the "Green Light" for the MainActivity Router
        editor.putBoolean("setup_complete", true)
        editor.apply()
    }

    fun getTagKeys(tagId: String): Map<Int, ByteArray>? {
        val keys = mutableMapOf<Int, ByteArray>()
        for (i in 0..15) {
            val hexKey = sharedPreferences.getString("${tagId}_sector_$i", null) ?: continue
            keys[i] = hexKey.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }
        return if (keys.isEmpty()) null else keys
    }

    /**
     * Wipe everything for troubleshooting or re-setup
     */
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}