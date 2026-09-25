package com.wordpopup.app

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Quét nội dung chữ đang hiển thị trên màn hình (mọi app khác), tìm từ có trong
 * VocabRepository và vẽ highlight đúng vị trí bằng OverlayController.
 */
class WordScanAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var pendingScan: Runnable? = null
    private val scanDelayMs = 600L
    private val tokenRegex = Regex("[A-Za-z]+(?:-[A-Za-z]+)*")

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 200
        }
        OverlayController.attach(this)
        if (VocabRepository.entries.isEmpty()) {
            Thread { VocabRepository.loadFromSavedFolder(applicationContext) }.start()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        pendingScan?.let { handler.removeCallbacks(it) }
        val r = Runnable { scanScreen() }
        pendingScan = r
        handler.postDelayed(r, scanDelayMs)
    }

    override fun onInterrupt() { }

    private fun scanScreen() {
        if (VocabRepository.entries.isEmpty()) return
        val root = rootInActiveWindow ?: return
        val matches = mutableListOf<OverlayController.Match>()
        collectMatches(root, matches, 0)
        OverlayController.updateHighlights(matches)
    }

    private fun collectMatches(node: AccessibilityNodeInfo, out: MutableList<OverlayController.Match>, depth: Int) {
        if (depth > 60 || out.size > 300) return
        val text = node.text?.toString()
        if (!text.isNullOrBlank()) {
            findWordMatches(node, text, out)
        }
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            collectMatches(child, out, depth + 1)
        }
    }

    private fun findWordMatches(node: AccessibilityNodeInfo, text: String, out: MutableList<OverlayController.Match>) {
        for (m in tokenRegex.findAll(text)) {
            val word = m.value.lowercase()
            val entry = VocabRepository.entries[word] ?: continue
            val rect = getCharRangeBounds(node, m.range.first, m.range.last + 1)
                ?: nodeBoundsFallback(node)
            if (rect != null) {
                out.add(OverlayController.Match(word, entry, rect))
            }
        }
    }

    private fun nodeBoundsFallback(node: AccessibilityNodeInfo): Rect? {
        val r = Rect()
        node.getBoundsInScreen(r)
        return if (r.isEmpty) null else r
    }

    /** Lấy vị trí chính xác của 1 cụm ký tự (per-word) trong node, dùng API 26+. */
    private fun getCharRangeBounds(node: AccessibilityNodeInfo, start: Int, end: Int): Rect? {
        return try {
            val args = Bundle()
            args.putInt(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_START_INDEX, start)
            args.putInt(AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_ARG_LENGTH, end - start)
            val ok = node.refreshWithExtraData(
                AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY, args
            )
            if (!ok) return null
            val key = AccessibilityNodeInfo.EXTRA_DATA_TEXT_CHARACTER_LOCATION_KEY
            val parcelables = node.extras?.getParcelableArray(key) ?: return null
            var union: RectF? = null
            for (p in parcelables) {
                val rf = p as? RectF ?: continue
                if (rf.width() <= 0f && rf.height() <= 0f) continue
                if (union == null) union = RectF(rf) else union.union(rf)
            }
            union?.let { Rect(it.left.toInt(), it.top.toInt(), it.right.toInt(), it.bottom.toInt()) }
        } catch (e: Exception) {
            null
        }
    }
}
