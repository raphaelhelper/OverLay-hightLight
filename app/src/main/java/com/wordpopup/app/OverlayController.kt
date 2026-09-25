package com.wordpopup.app

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/**
 * Vẽ các ô highlight (mỗi ô là 1 overlay window nhỏ loại TYPE_ACCESSIBILITY_OVERLAY —
 * loại này KHÔNG cần quyền "hiển thị trên ứng dụng khác", chỉ cần AccessibilityService
 * đang bật) đúng vị trí từ vựng trên màn hình, và popup nghĩa khi tap vào 1 ô.
 */
object OverlayController {

    data class Match(val word: String, val entry: VocabEntry, val rect: Rect)

    private var windowManager: WindowManager? = null
    private var context: Context? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val highlightViews = mutableListOf<View>()
    private var popupView: View? = null
    private var popupWord: String? = null

    fun attach(service: AccessibilityService) {
        context = service
        windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun updateHighlights(matches: List<Match>) {
        mainHandler.post {
            val ctx = context ?: return@post
            val wm = windowManager ?: return@post

            for (v in highlightViews) {
                try { wm.removeView(v) } catch (e: Exception) { }
            }
            highlightViews.clear()

            val deduped = matches.distinctBy {
                "${it.word}_${it.rect.left}_${it.rect.top}_${it.rect.right}_${it.rect.bottom}"
            }

            for (m in deduped) {
                if (m.rect.width() <= 0 || m.rect.height() <= 0) continue
                val box = View(ctx)
                val bg = GradientDrawable()
                bg.setColor(Color.parseColor("#552ECC71"))
                bg.cornerRadius = 6f
                box.background = bg
                box.setOnClickListener { toggleClick(m) }

                val params = WindowManager.LayoutParams(
                    m.rect.width(),
                    m.rect.height(),
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                )
                params.gravity = Gravity.TOP or Gravity.START
                params.x = m.rect.left
                params.y = m.rect.top

                try {
                    wm.addView(box, params)
                    highlightViews.add(box)
                } catch (e: Exception) { }
            }
        }
    }

    private fun toggleClick(m: Match) {
        if (popupWord == m.word && popupView != null) {
            closePopup()
        } else {
            showPopup(m)
        }
    }

    private fun showPopup(m: Match) {
        val ctx = context ?: return
        val wm = windowManager ?: return
        closePopup()

        val inflater = LayoutInflater.from(ctx)
        val view = inflater.inflate(R.layout.popup_meaning, null)
        view.findViewById<TextView>(R.id.popupWord).text = m.entry.word
        view.findViewById<TextView>(R.id.popupMeaning).text = m.entry.meaningVi
        view.findViewById<TextView>(R.id.popupExample).text = m.entry.example
        view.findViewById<TextView>(R.id.popupExampleVi).text = m.entry.exampleVi
        view.findViewById<View>(R.id.popupClose).setOnClickListener { closePopup() }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        val screenWidth = ctx.resources.displayMetrics.widthPixels
        val screenHeight = ctx.resources.displayMetrics.heightPixels
        val desiredY = m.rect.bottom + 8
        params.x = m.rect.left.coerceIn(0, (screenWidth - 40).coerceAtLeast(0))
        params.y = if (desiredY + 220 > screenHeight) (m.rect.top - 228).coerceAtLeast(0) else desiredY

        try {
            wm.addView(view, params)
            popupView = view
            popupWord = m.word
        } catch (e: Exception) { }
    }

    private fun closePopup() {
        val wm = windowManager ?: return
        popupView?.let {
            try { wm.removeView(it) } catch (e: Exception) { }
        }
        popupView = null
        popupWord = null
    }
}
