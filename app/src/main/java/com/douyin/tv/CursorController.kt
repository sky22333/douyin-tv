package com.douyin.tv

import android.annotation.SuppressLint
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible

class CursorController(
    private val root: View,
    private val webView: WebView,
    private val cursorView: ImageView,
    private val hintView: TextView
) {
    var isActive = false
        private set

    private var cursorX = 0f
    private var cursorY = 0f

    fun enter() {
        isActive = true
        cursorView.isVisible = true
        hintView.isVisible = true
        webView.post {
            cursorX = (root.width - cursorView.width) / 2f
            cursorY = (root.height - cursorView.height) / 2f
            cursorView.translationX = cursorX
            cursorView.translationY = cursorY
        }
        webView.requestFocus()
    }

    fun exit() {
        isActive = false
        cursorView.isVisible = false
        hintView.isVisible = false
        webView.requestFocus()
    }

    fun move(event: KeyEvent, dx: Float, dy: Float) {
        val step = cursorStepPx(event)
        val maxX = (root.width - cursorView.width).toFloat().coerceAtLeast(0f)
        val maxY = (root.height - cursorView.height).toFloat().coerceAtLeast(0f)
        cursorX = (cursorX + dx * step).coerceIn(0f, maxX)
        cursorY = (cursorY + dy * step).coerceIn(0f, maxY)
        cursorView.translationX = cursorX
        cursorView.translationY = cursorY
    }

    @SuppressLint("ClickableViewAccessibility")
    fun click() {
        webView.post {
            val width = webView.width
            val height = webView.height
            if (width <= 0 || height <= 0) return@post

            val rootLoc = IntArray(2)
            val webLoc = IntArray(2)
            root.getLocationOnScreen(rootLoc)
            webView.getLocationOnScreen(webLoc)

            val x = (cursorX + cursorView.width / 2f - (webLoc[0] - rootLoc[0]))
                .coerceIn(0f, width.toFloat() - 1f)
            val y = (cursorY + cursorView.height / 2f - (webLoc[1] - rootLoc[1]))
                .coerceIn(0f, height.toFloat() - 1f)

            val downTime = SystemClock.uptimeMillis()
            val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
            webView.dispatchTouchEvent(down)
            down.recycle()

            val up = MotionEvent.obtain(downTime, downTime + TAP_UP_DELAY_MS, MotionEvent.ACTION_UP, x, y, 0)
            webView.dispatchTouchEvent(up)
            up.recycle()
        }
    }

    private fun cursorStepPx(event: KeyEvent): Float {
        val dp =
            if (event.repeatCount == 0) {
                CURSOR_STEP_INITIAL_DP
            } else {
                (CURSOR_STEP_INITIAL_DP + event.repeatCount * CURSOR_STEP_ACCEL_PER_REPEAT_DP)
                    .coerceAtMost(CURSOR_STEP_MAX_DP)
            }
        return dp * root.resources.displayMetrics.density
    }

    private companion object {
        const val CURSOR_STEP_INITIAL_DP = 2.2f
        const val CURSOR_STEP_ACCEL_PER_REPEAT_DP = 0.85f
        const val CURSOR_STEP_MAX_DP = 12f
        const val TAP_UP_DELAY_MS = 50L
    }
}
