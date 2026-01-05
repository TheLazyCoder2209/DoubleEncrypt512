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
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "secure_nfc_keys_v1",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun isInitialized(): Boolean {
        // Checking for setup_complete is more reliable than checking if any keys exist
        return sharedPreferences.getBoolean("setup_complete", false)
    }

    /**
     * Saves the default directory URI and takes persistable permission
     * so the app can access it again after a reboot.
     */
    fun setDefaultDirectory(uri: Uri) {
        try {
            val contentResolver = context.contentResolver
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

            // This is the "Magic" part: it asks Android to keep the permission forever
            contentResolver.takePersistableUriPermission(uri, takeFlags)

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

    // New helper to initialize the vault during the Wizard
    fun initializeWithTag(tagId: String): Boolean {
        // Logic to generate initial keys or just mark setup as done
        // For now, we'll mark setup as true when a tag is first linked
        sharedPreferences.edit().putBoolean("setup_complete", true).apply()
        return true
    }
}