package com.lazycoder.doubleencrypt512

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class SecurityManager {

    private val KEY_ALIAS = "DoubleEncrypt512_Master_Key"
    private val PROVIDER = "AndroidKeyStore"

    fun getOrCreateHardwareKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }

        // If the key already exists in the TrustZone, return it
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        // If not, generate a new one inside the hardware
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true) // This triggers the Fingerprint/PIN
            .setUserAuthenticationValidityDurationSeconds(300) // Stays unlocked for 5 mins
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}