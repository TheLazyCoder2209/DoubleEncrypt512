package com.lazycoder.doubleencrypt512

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var nfcStatusText: TextView
    private lateinit var dirPathText: TextView
    private lateinit var secureDeleteCheck: CheckBox
    private lateinit var vaultStorage: VaultStorage
    private lateinit var cryptoEngine: CryptoEngine
    private var nfcAdapter: NfcAdapter? = null
    private var pendingUri: Uri? = null
    private var isEncryptMode = true

    // OpenDocument grants WRITE/DELETE permissions which GetContent does not
    private val getFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            pendingUri = it
            nfcStatusText.text = "FILE READY: TAP NFC TAG"
            nfcStatusText.setTextColor(getColor(android.R.color.holo_orange_light))
        }
    }

    private val selectDir = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            vaultStorage.setDefaultDirectory(it)
            updateDirUI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vaultStorage = VaultStorage(this)
        cryptoEngine = CryptoEngine(this)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (!vaultStorage.isInitialized()) {
            startActivity(Intent(this, SetupWizardActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        nfcStatusText = findViewById(R.id.nfc_status_text)
        dirPathText = findViewById(R.id.dir_path_text)
        secureDeleteCheck = findViewById(R.id.check_secure_delete)

        vaultStorage.getDefaultDirectory()?.let { updateDirUI(it) }

        findViewById<Button>(R.id.btn_encrypt).setOnClickListener {
            isEncryptMode = true
            getFile.launch(arrayOf("*/*"))
        }

        findViewById<Button>(R.id.btn_decrypt).setOnClickListener {
            isEncryptMode = false
            getFile.launch(arrayOf("*/*"))
        }

        findViewById<Button>(R.id.btn_select_dir).setOnClickListener { selectDir.launch(null) }
    }

    private fun updateDirUI(uri: Uri) {
        dirPathText.text = "Output: ${uri.path?.split(":")?.lastOrNull()}"
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        if (tag != null && pendingUri != null) {
            val tagId = tag.id.joinToString("") { "%02x".format(it) }
            val key = vaultStorage.getTagKeys(tagId)?.get(1)
            val targetDir = vaultStorage.getDefaultDirectory()

            if (targetDir == null) {
                Toast.makeText(this, "Select output folder first!", Toast.LENGTH_SHORT).show()
                return
            }

            if (key != null) {
                val resultUri = if (isEncryptMode) {
                    cryptoEngine.encryptFile(pendingUri!!, key, targetDir)
                } else {
                    cryptoEngine.decryptFile(pendingUri!!, key, targetDir)
                }

                if (resultUri != null) {
                    if (isEncryptMode && secureDeleteCheck.isChecked) {
                        cryptoEngine.secureErase(pendingUri!!)
                    }
                    nfcStatusText.text = "SUCCESS"
                    nfcStatusText.setTextColor(getColor(android.R.color.holo_green_light))
                    pendingUri = null
                } else {
                    nfcStatusText.text = "ERROR: ACTION FAILED"
                    nfcStatusText.setTextColor(getColor(android.R.color.holo_red_light))
                }
            }
        }
    }
}