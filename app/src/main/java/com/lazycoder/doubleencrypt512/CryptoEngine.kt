package com.lazycoder.doubleencrypt512

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoEngine(private val context: Context) {

    private val ALGORITHM = "AES/GCM/NoPadding"
    private val TAG_BIT_LENGTH = 128
    private val IV_BYTE_LENGTH = 12

    /**
     * True 512-bit Cascade:
     * Key A (256-bit) -> AES-GCM (Layer 1)
     * Key B (256-bit) -> AES-GCM (Layer 2)
     */
    fun encryptFile(sourceUri: Uri, key512: ByteArray, outputDirUri: Uri): Uri? {
        if (key512.size < 64) return null // Ensure we actually have 512 bits

        return try {
            // Split the 512-bit key into two 256-bit keys
            val keyA = key512.sliceArray(0 until 32)
            val keyB = key512.sliceArray(32 until 64)

            val sourceName = getFileName(sourceUri) ?: "vault_data"
            val outputName = "$sourceName.vlt"

            val pickedDir = DocumentFile.fromTreeUri(context, outputDirUri)
            val newFile = pickedDir?.createFile("application/octet-stream", outputName)
            val resultUri = newFile?.uri ?: return null

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                context.contentResolver.openOutputStream(resultUri)?.use { output ->

                    // Generate two unique IVs
                    val ivA = ByteArray(IV_BYTE_LENGTH).apply { SecureRandom().nextBytes(this) }
                    val ivB = ByteArray(IV_BYTE_LENGTH).apply { SecureRandom().nextBytes(this) }

                    // Write IVs to the header so we can decrypt later
                    output.write(ivA)
                    output.write(ivB)

                    // LAYER 1: Inner Encryption
                    val cipherA = Cipher.getInstance(ALGORITHM).apply {
                        init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyA, "AES"), GCMParameterSpec(TAG_BIT_LENGTH, ivA))
                    }

                    // LAYER 2: Outer Encryption
                    val cipherB = Cipher.getInstance(ALGORITHM).apply {
                        init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyB, "AES"), GCMParameterSpec(TAG_BIT_LENGTH, ivB))
                    }

                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        // Cascade: Input -> Cipher A -> Cipher B -> Disk
                        val layer1 = cipherA.update(buffer, 0, read)
                        if (layer1 != null) {
                            val layer2 = cipherB.update(layer1)
                            if (layer2 != null) output.write(layer2)
                        }
                    }

                    // Finalize the cascade
                    val finalA = cipherA.doFinal()
                    val finalB = cipherB.doFinal(finalA)
                    output.write(finalB)
                }
            }
            resultUri
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    fun decryptFile(sourceUri: Uri, key512: ByteArray, outputDirUri: Uri): Uri? {
        if (key512.size < 64) return null

        return try {
            val keyA = key512.sliceArray(0 until 32)
            val keyB = key512.sliceArray(32 until 64)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                // Read the two IVs from the header
                val ivA = ByteArray(IV_BYTE_LENGTH)
                val ivB = ByteArray(IV_BYTE_LENGTH)
                if (input.read(ivA) != IV_BYTE_LENGTH) return null
                if (input.read(ivB) != IV_BYTE_LENGTH) return null

                // Initialize Ciphers in Reverse Order (B then A)
                val cipherB = Cipher.getInstance(ALGORITHM).apply {
                    init(Cipher.DECRYPT_MODE, SecretKeySpec(keyB, "AES"), GCMParameterSpec(TAG_BIT_LENGTH, ivB))
                }
                val cipherA = Cipher.getInstance(ALGORITHM).apply {
                    init(Cipher.DECRYPT_MODE, SecretKeySpec(keyA, "AES"), GCMParameterSpec(TAG_BIT_LENGTH, ivA))
                }

                val sourceName = getFileName(sourceUri) ?: "restored_file"
                val outputName = sourceName.replace(".vlt", "")

                val pickedDir = DocumentFile.fromTreeUri(context, outputDirUri)
                val newFile = pickedDir?.createFile("application/octet-stream", outputName)
                val resultUri = newFile?.uri ?: return null

                context.contentResolver.openOutputStream(resultUri)?.use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        // Reverse Cascade: Disk -> Decrypt B -> Decrypt A -> Output
                        val layerB = cipherB.update(buffer, 0, read)
                        if (layerB != null) {
                            val layerA = cipherA.update(layerB)
                            if (layerA != null) output.write(layerA)
                        }
                    }
                    val finalB = cipherB.doFinal()
                    val finalA = cipherA.doFinal(finalB)
                    output.write(finalA)
                }
                resultUri
            }
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    fun secureErase(uri: Uri) {
        try {
            context.contentResolver.openFileDescriptor(uri, "rwt")?.use { pfd ->
                val fileSize = pfd.statSize
                if (fileSize > 0) {
                    val fos = FileOutputStream(pfd.fileDescriptor)
                    val random = SecureRandom()
                    val buffer = ByteArray(8192)

                    // Pass 1: Random, Pass 2: Zeros
                    for (pass in 1..2) {
                        var written = 0L
                        while (written < fileSize) {
                            if (pass == 1) random.nextBytes(buffer) else buffer.fill(0)
                            val toWrite = if (fileSize - written < buffer.size) (fileSize - written).toInt() else buffer.size
                            fos.write(buffer, 0, toWrite)
                            written += toWrite
                        }
                        pfd.fileDescriptor.sync()
                        if (pass == 1) fos.channel.position(0)
                    }
                }
            }
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun getFileName(uri: Uri): String? = DocumentFile.fromSingleUri(context, uri)?.name
}