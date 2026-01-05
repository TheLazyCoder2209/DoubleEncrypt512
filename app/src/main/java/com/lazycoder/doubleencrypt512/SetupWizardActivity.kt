package com.lazycoder.doubleencrypt512

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SetupWizardActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var vaultStorage: VaultStorage
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_wizard)

        // FIX: Changed from wizardStatusText to wizardDescription to match your XML
        statusText = findViewById(R.id.wizardDescription)

        vaultStorage = VaultStorage(this)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        findViewById<android.view.View>(R.id.btnCancelWizard).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.let {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            it.enableForegroundDispatch(this, pendingIntent, null, null)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Modern way to get the Parcelable for Android 12/13+ compatibility
        val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        if (tag != null) {
            val manager = TagManager()
            val keys = manager.readTagData(tag)
            if (keys != null) {
                vaultStorage.saveTagKeys(manager.getTagIdHex(tag), keys)

                // Update the UI to show progress
                statusText.text = "✅ TAG REGISTERED\nYour vault is now linked. Redirecting to Main Dashboard..."
                statusText.setTextColor(getColor(android.R.color.holo_green_light))

                statusText.postDelayed({
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }, 1500)
            } else {
                statusText.text = "❌ ERROR: Could not read tag. Ensure it is a compatible NFC tag."
                statusText.setTextColor(getColor(android.R.color.holo_red_light))
            }
        }
    }
}