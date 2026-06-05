package com.douyin.tv

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.ViewConfiguration

class RemoteKeyController(
    private val shortcutDispatcher: ShortcutDispatcher,
    private val cursorController: CursorController,
    private val isMenuVisible: () -> Boolean,
    private val showMenu: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var longPressMenuFired = false
    private val okLongPressTimeoutMs =
        ViewConfiguration.getLongPressTimeout().toLong().coerceAtLeast(OK_LONG_PRESS_FALLBACK_MS)
    private val showMenuAfterLongPressOk = Runnable {
        longPressMenuFired = true
        showMenu()
    }

    fun handle(event: KeyEvent): Boolean {
        if (isMenuVisible()) return false
        return if (cursorController.isActive) handleCursorMode(event) else handleBrowseMode(event)
    }

    fun release() {
        mainHandler.removeCallbacks(showMenuAfterLongPressOk)
    }

    private fun handleCursorMode(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> moveCursor(event, 0f, -1f)
            KeyEvent.KEYCODE_DPAD_DOWN -> moveCursor(event, 0f, 1f)
            KeyEvent.KEYCODE_DPAD_LEFT -> moveCursor(event, -1f, 0f)
            KeyEvent.KEYCODE_DPAD_RIGHT -> moveCursor(event, 1f, 0f)
            else -> {
                if (isOkKey(event.keyCode)) {
                    cursorController.click()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun handleBrowseMode(event: KeyEvent): Boolean {
        return when (event.action) {
            KeyEvent.ACTION_DOWN -> handleBrowseKeyDown(event)
            KeyEvent.ACTION_UP -> handleBrowseKeyUp(event)
            else -> false
        }
    }

    private fun handleBrowseKeyDown(event: KeyEvent): Boolean {
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> dispatchShortcut(Shortcut.Previous)
            KeyEvent.KEYCODE_DPAD_DOWN -> dispatchShortcut(Shortcut.Next)
            KeyEvent.KEYCODE_DPAD_LEFT -> dispatchShortcut(Shortcut.LeftAction)
            KeyEvent.KEYCODE_DPAD_RIGHT -> dispatchShortcut(Shortcut.RightAction)
            else -> {
                if (isOkKey(event.keyCode)) {
                    if (event.repeatCount == 0) {
                        longPressMenuFired = false
                        mainHandler.removeCallbacks(showMenuAfterLongPressOk)
                        mainHandler.postDelayed(showMenuAfterLongPressOk, okLongPressTimeoutMs)
                    }
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun handleBrowseKeyUp(event: KeyEvent): Boolean {
        if (!isOkKey(event.keyCode)) return false

        mainHandler.removeCallbacks(showMenuAfterLongPressOk)
        val canceled = (event.flags and KeyEvent.FLAG_CANCELED) != 0
        if (!longPressMenuFired && !canceled) {
            shortcutDispatcher.dispatch(Shortcut.OkAction)
        }
        longPressMenuFired = false
        return true
    }

    private fun moveCursor(event: KeyEvent, dx: Float, dy: Float): Boolean {
        cursorController.move(event, dx, dy)
        return true
    }

    private fun dispatchShortcut(shortcut: Shortcut): Boolean {
        shortcutDispatcher.dispatch(shortcut)
        return true
    }

    private fun isOkKey(keyCode: Int): Boolean =
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_BUTTON_A,
            KeyEvent.KEYCODE_BUTTON_SELECT -> true
            else -> false
        }

    private companion object {
        const val OK_LONG_PRESS_FALLBACK_MS = 500L
    }
}
