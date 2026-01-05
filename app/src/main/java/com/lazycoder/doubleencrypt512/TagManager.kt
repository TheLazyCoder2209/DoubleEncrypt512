package com.lazycoder.doubleencrypt512

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.util.Log

class TagManager {

    /**
     * Reads all 16 sectors of a Mifare Classic Tag.
     * In a "DoubleEncrypt" scenario, we use the tag's internal data as
     * the second layer of the 512-bit encryption key.
     */
    fun readTagData(tag: Tag): List<ByteArray>? {
        val mifare = MifareClassic.get(tag) ?: return null
        val tagData = mutableListOf<ByteArray>()

        try {
            mifare.connect()

            // Mifare Classic 1K has 16 sectors
            for (i in 0 until 16) {
                // We attempt to authenticate with the Factory Default Key (Key A)
                // In a production app, you might use a custom key here.
                val authenticated = mifare.authenticateSectorWithKeyA(i, MifareClassic.KEY_DEFAULT)

                if (authenticated) {
                    // Read the first block of each sector (16 bytes)
                    val blockIndex = mifare.sectorToBlock(i)
                    val blockData = mifare.readBlock(blockIndex)
                    tagData.add(blockData)
                } else {
                    Log.e("TagManager", "Authentication failed for sector $i")
                    // If we can't read a sector, we add a dummy to keep indexes aligned
                    tagData.add(ByteArray(16))
                }
            }
        } catch (e: Exception) {
            Log.e("TagManager", "Error reading tag: ${e.message}")
            return null
        } finally {
            mifare.close()
        }

        return tagData
    }

    /**
     * Helper to convert the byte ID of the tag to a Hex String
     */
    fun getTagIdHex(tag: Tag): String {
        return tag.id.joinToString("") { "%02x".format(it) }
    }
}