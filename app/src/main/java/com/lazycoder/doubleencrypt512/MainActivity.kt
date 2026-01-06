package com.lazycoder.doubleencrypt512

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var nfcStatusText: TextView
    private lateinit var terminalLog: TextView
    private lateinit var logScrollView: ScrollView
    private lateinit var secureDeleteCheck: CheckBox
    private lateinit var dirPathText: TextView

    private lateinit var vaultStorage: VaultStorage
    private lateinit var cryptoEngine: CryptoEngine
    private var nfcAdapter: NfcAdapter? = null

    private var pendingUri: Uri? = null
    private var isEncryptMode = true

    // --- HELPER FOR GLOBAL BOLDING ---
    private fun applyOverkillBold(rawText: String): CharSequence {
        val cleanText = rawText.replace("****", "")
        val builder = SpannableStringBuilder(cleanText)
        val firstMarker = rawText.indexOf("****")
        val lastMarker = rawText.lastIndexOf("****")

        if (firstMarker != -1 && lastMarker != -1 && firstMarker != lastMarker) {
            val start = firstMarker
            val end = lastMarker - 4
            builder.setSpan(StyleSpan(Typeface.BOLD),
                start.coerceAtLeast(0),
                end.coerceAtMost(cleanText.length), 0)
        }
        return builder
    }

    private val getFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            pendingUri = it
            appendLog("****[USER]**** File staged: ${it.path?.split("/")?.lastOrNull()}")
            // Apply bold fix to the status bar
            nfcStatusText.text = applyOverkillBold("****READY: TAP TAG****")
        }
    }

    private val selectDir = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            vaultStorage.setDefaultDirectory(it)
            dirPathText.text = "Output: ${it.path?.split(":")?.lastOrNull()}"
            appendLog("****[SYSTEM]**** Output directory updated.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vaultStorage = VaultStorage(this)
        if (!vaultStorage.isInitialized()) {
            startActivity(Intent(this, SetupWizardActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        cryptoEngine = CryptoEngine(this)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        nfcStatusText = findViewById(R.id.nfc_status_text)
        terminalLog = findViewById(R.id.terminal_log)
        logScrollView = findViewById(R.id.log_scroll_view)
        secureDeleteCheck = findViewById(R.id.check_secure_delete)
        dirPathText = findViewById(R.id.dir_path_text)

        appendLog("****[SYSTEM]**** DoubleEncrypt512 Engine Init...")

        vaultStorage.getDefaultDirectory()?.let {
            dirPathText.text = "Output: ${it.path?.split(":")?.lastOrNull()}"
        }

        findViewById<Button>(R.id.btn_encrypt).setOnClickListener {
            isEncryptMode = true
            getFile.launch(arrayOf("*/*"))
        }

        findViewById<Button>(R.id.btn_decrypt).setOnClickListener {
            isEncryptMode = false
            getFile.launch(arrayOf("*/*"))
        }

        findViewById<Button>(R.id.btn_select_dir).setOnClickListener {
            selectDir.launch(null)
        }
    }

    private fun appendLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        runOnUiThread {
            if (::terminalLog.isInitialized) {
                terminalLog.append("\n")
                terminalLog.append(applyOverkillBold("[$time] $message"))
                logScrollView.post { logScrollView.fullScroll(View.FOCUS_DOWN) }
            }
        }
    }

    private fun showBiometricGate(tagId: String) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    appendLog("****[AUTH]**** Match. Releasing Keys...")
                    executeCryptoProcess(tagId)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    appendLog("****[ERROR]**** Denied: $errString")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authorize Transaction")
            .setSubtitle("512-bit key release requested.")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun executeCryptoProcess(tagId: String) {
        val key = vaultStorage.getTagKeys(tagId)?.get(1)
        val targetDir = vaultStorage.getDefaultDirectory()

        if (key == null || targetDir == null) {
            appendLog("****[ERROR]**** Key or Directory missing.")
            return
        }

        val sourceUri = pendingUri ?: return
        appendLog("****[EE]**** Cascading ${if (isEncryptMode) "Encryption" else "Decryption"}...")

        val resultUri = if (isEncryptMode) {
            cryptoEngine.encryptFile(sourceUri, key, targetDir)
        } else {
            cryptoEngine.decryptFile(sourceUri, key, targetDir)
        }

        if (resultUri != null) {
            appendLog("****[SUCCESS]**** Operation Complete.")
            if (isEncryptMode && secureDeleteCheck.isChecked) {
                cryptoEngine.secureErase(sourceUri)
                appendLog("****[SECURE]**** Shredded source.")
            }
            pendingUri = null
            nfcStatusText.text = applyOverkillBold("****TRANSACTION COMPLETE****")
        } else {
            appendLog("****[FATAL]**** Engine failure.")
        }
    }

    override fun onResume() {
        super.onResume()
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        if (tag != null) {
            if (pendingUri == null) {
                appendLog("****[SYSTEM]**** No file selected.")
                return
            }
            val tagId = tag.id.joinToString("") { "%02x".format(it) }
            appendLog("****[NFC]**** Tag Found: $tagId")
            showBiometricGate(tagId)
        }
    }
}