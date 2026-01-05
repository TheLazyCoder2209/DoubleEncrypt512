package com.lazycoder.doubleencrypt512

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoEngine(private val context: Context) {

    private val ALGORITHM = "AES/GCM/NoPadding"
    private val TAG_BIT_LENGTH = 128
    private val IV_BYTE_LENGTH = 12

    fun encryptFile(sourceUri: Uri, keyBytes: ByteArray, outputDirUri: Uri): Uri? {
        return try {
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance(ALGORITHM)
            val iv = ByteArray(IV_BYTE_LENGTH)
            SecureRandom().nextBytes(iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_BIT_LENGTH, iv))

            val sourceName = getFileName(sourceUri) ?: "secret"
            val outputName = "$sourceName.dec"
            val pickedDir = DocumentFile.fromTreeUri(context, outputDirUri)
            val newFile = pickedDir?.createFile("application/octet-stream", outputName)
            val resultUri = newFile?.uri ?: return null

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                context.contentResolver.openOutputStream(resultUri)?.use { output ->
                    output.write(iv)
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        val out = cipher.update(buffer, 0, read)
                        if (out != null) output.write(out)
                    }
                    output.write(cipher.doFinal())
                }
            }
            resultUri
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    fun decryptFile(sourceUri: Uri, keyBytes: ByteArray, outputDirUri: Uri): Uri? {
        return try {
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance(ALGORITHM)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                val iv = ByteArray(IV_BYTE_LENGTH)
                if (input.read(iv) != IV_BYTE_LENGTH) return null
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_BIT_LENGTH, iv))

                val sourceName = getFileName(sourceUri) ?: "decrypted"
                val outputName = sourceName.replace(".dec", "")
                val pickedDir = DocumentFile.fromTreeUri(context, outputDirUri)
                val newFile = pickedDir?.createFile("application/octet-stream", outputName)
                val resultUri = newFile?.uri ?: return null

                context.contentResolver.openOutputStream(resultUri)?.use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        val out = cipher.update(buffer, 0, read)
                        if (out != null) output.write(out)
                    }
                    output.write(cipher.doFinal())
                }
                resultUri
            }
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    fun secureErase(uri: Uri) {
        try {
            // 1. Overwrite with random data
            context.contentResolver.openFileDescriptor(uri, "rwt")?.use { pfd ->
                val fileSize = pfd.statSize
                if (fileSize > 0) {
                    val fos = FileOutputStream(pfd.fileDescriptor)
                    val random = SecureRandom()
                    val buffer = ByteArray(8192)
                    var written = 0L
                    while (written < fileSize) {
                        random.nextBytes(buffer)
                        val toWrite = if (fileSize - written < buffer.size) (fileSize - written).toInt() else buffer.size
                        fos.write(buffer, 0, toWrite)
                        written += toWrite
                    }
                    fos.flush()
                    pfd.fileDescriptor.sync() // Force physical write
                }
            }
            // 2. Actual Deletion
            if (!DocumentsContract.deleteDocument(context.contentResolver, uri)) {
                context.contentResolver.delete(uri, null, null)
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun getFileName(uri: Uri): String? = DocumentFile.fromSingleUri(context, uri)?.name
}