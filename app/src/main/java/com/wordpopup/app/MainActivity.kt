package com.wordpopup.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var statusText: TextView

    private val pickFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            prefs.edit().putString(KEY_FOLDER_URI, uri.toString()).apply()
            Toast.makeText(this, "Đã lưu thư mục, đang tải danh sách từ...", Toast.LENGTH_SHORT).show()
            loadVocab(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        statusText = findViewById(R.id.statusText)

        findViewById<Button>(R.id.btnPickFolder).setOnClickListener {
            pickFolder.launch(null)
        }
        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.btnReload).setOnClickListener {
            val uriStr = prefs.getString(KEY_FOLDER_URI, null)
            if (uriStr == null) {
                Toast.makeText(this, "Chưa chọn thư mục", Toast.LENGTH_SHORT).show()
            } else {
                loadVocab(Uri.parse(uriStr))
            }
        }

        val savedUri = prefs.getString(KEY_FOLDER_URI, null)
        if (savedUri != null && VocabRepository.entries.isEmpty()) {
            loadVocab(Uri.parse(savedUri))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun loadVocab(uri: Uri) {
        Thread {
            val count = VocabRepository.loadFromFolder(applicationContext, uri)
            runOnUiThread {
                Toast.makeText(this, "Đã tải $count từ", Toast.LENGTH_LONG).show()
                refreshStatus()
            }
        }.start()
    }

    private fun refreshStatus() {
        val folderOk = prefs.getString(KEY_FOLDER_URI, null) != null
        val accessOk = isAccessibilityServiceEnabled()
        val wordCount = VocabRepository.entries.size
        statusText.text = buildString {
            append(if (folderOk) "✅ Đã chọn thư mục\n" else "❌ Chưa chọn thư mục\n")
            append(if (accessOk) "✅ Đã bật Accessibility\n" else "❌ Chưa bật Accessibility (bắt buộc)\n")
            append("📚 Số từ đã tải: $wordCount")
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == packageName &&
                    it.resolveInfo.serviceInfo.name == WordScanAccessibilityService::class.java.name
        }
    }

    companion object {
        const val PREFS_NAME = "word_popup_prefs"
        const val KEY_FOLDER_URI = "folder_uri"
    }
}
