package com.wordpopup.app

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.InputStreamReader

data class VocabEntry(
    val word: String,
    val meaningVi: String,
    val example: String,
    val exampleVi: String
)

/**
 * Đọc ~200 file .txt trong 1 thư mục (đúng định dạng người dùng cung cấp) và
 * giữ trong bộ nhớ (singleton) để AccessibilityService tra cứu nhanh khi quét màn hình.
 *
 * Định dạng mỗi mục trong file, các dòng trống bị bỏ qua khi parse:
 *   1.
 *   <word>
 *   <nghĩa tiếng Việt>
 *   Example: <câu ví dụ tiếng Anh>
 *   Dịch: <dịch câu ví dụ>
 */
object VocabRepository {

    @Volatile
    var entries: Map<String, VocabEntry> = emptyMap()
        private set

    private val numberLine = Regex("^\\d+\\.\\s*$")

    fun loadFromFolder(context: Context, treeUri: Uri): Int {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return 0
        val map = HashMap<String, VocabEntry>()
        val files = root.listFiles().filter {
            it.isFile && (it.name?.endsWith(".txt", ignoreCase = true) == true)
        }
        for (file in files) {
            try {
                context.contentResolver.openInputStream(file.uri)?.use { input ->
                    parseFile(input, map)
                }
            } catch (e: Exception) {
                // bỏ qua file lỗi, tiếp tục với các file còn lại
            }
        }
        entries = map
        return map.size
    }

    fun loadFromSavedFolder(context: Context): Int {
        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val uriStr = prefs.getString(MainActivity.KEY_FOLDER_URI, null) ?: return 0
        return loadFromFolder(context, Uri.parse(uriStr))
    }

    private fun parseFile(input: java.io.InputStream, out: HashMap<String, VocabEntry>) {
        val reader = BufferedReader(InputStreamReader(input, Charsets.UTF_8))
        val lines = reader.readLines().map { it.trim() }.filter { it.isNotEmpty() }
        var i = 0
        while (i < lines.size) {
            if (numberLine.matches(lines[i])) {
                val block = mutableListOf<String>()
                var j = i + 1
                while (j < lines.size && !numberLine.matches(lines[j])) {
                    block.add(lines[j])
                    j++
                }
                if (block.size >= 4) {
                    val word = block[0]
                    val meaning = block[1]
                    val example = block[2].removePrefix("Example:").trim()
                    val exampleVi = block[3].removePrefix("Dịch:").trim()
                    if (word.isNotEmpty()) {
                        out[word.lowercase()] = VocabEntry(word, meaning, example, exampleVi)
                    }
                }
                i = j
            } else {
                i++
            }
        }
    }
}
