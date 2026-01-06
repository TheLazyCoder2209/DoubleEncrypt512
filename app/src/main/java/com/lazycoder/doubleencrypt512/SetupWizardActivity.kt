package com.lazycoder.doubleencrypt512

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import java.security.SecureRandom

class SetupWizardActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var vaultStorage: VaultStorage
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_wizard)

        statusText = findViewById(R.id.wizardDescription)
        vaultStorage = VaultStorage(this)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        findViewById<View>(R.id.btnCancelWizard).setOnClickListener { finish() }
    }

    // This makes the ****text**** bold in your Holo UI
    private fun setStatusBold(rawText: String) {
        val cleanText = rawText.replace("****", "")
        val builder = SpannableStringBuilder(cleanText)
        val firstMarker = rawText.indexOf("****")
        val lastMarker = rawText.lastIndexOf("****")

        if (firstMarker != -1 && lastMarker != -1 && firstMarker != lastMarker) {
            val start = firstMarker
            val end = lastMarker - 4
            builder.setSpan(StyleSpan(android.graphics.Typeface.BOLD), start, end, 0)
        }
        statusText.text = builder
    }

    private fun authenticateForSetup(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    setStatusBold("****❌ AUTH FAILED:**** $errString")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Initialize 512-bit Vault")
            .setSubtitle("Bind identity to physical tag.")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.let {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_UPDATE_CURRENT
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
            it.enableForegroundDispatch(this, pendingIntent, null, null)
        }
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
            val tagId = tag.id.joinToString("") { "%02x".format(it) }
            setStatusBold("****NFC TAG DETECTED****\nScan finger to bind hardware...")

            authenticateForSetup {
                try {
                    val masterKey512 = ByteArray(64).apply { SecureRandom().nextBytes(this) }
                    val keyList = List(16) { i -> if (i == 1) masterKey512 else ByteArray(32) }

                    vaultStorage.saveTagKeys(tagId, keyList)

                    runOnUiThread {
                        setStatusBold("****✅ 2FA BIND COMPLETE****\nVault sealed to hardware.")
                        statusText.postDelayed({
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }, 2000)
                    }
                } catch (e: Exception) {
                    runOnUiThread { setStatusBold("****❌ STORAGE ERROR****") }
                }
            }
        }
    }
}