package com.douyin.tv

import android.os.SystemClock
import android.view.KeyEvent
import android.webkit.WebView

// 按键映射
enum class Shortcut(val keyCode: Int) {
    Previous(KeyEvent.KEYCODE_W), // 上一个视频
    Next(KeyEvent.KEYCODE_S), // 下一个视频
    LeftAction(KeyEvent.KEYCODE_R), // 左侧操作
    RightAction(KeyEvent.KEYCODE_Z), // 右侧操作
    OkAction(KeyEvent.KEYCODE_X), // 确认操作
    PlayerFullscreen(KeyEvent.KEYCODE_H), // 播放器全屏
    WebImmersive(KeyEvent.KEYCODE_Y), // 网页沉浸全屏
    PlayPause(KeyEvent.KEYCODE_SPACE), // 播放/暂停
    Autoplay(KeyEvent.KEYCODE_K), // 自动连播
    ClearScreen(KeyEvent.KEYCODE_J) // 清屏
}

class ShortcutDispatcher(private val webView: WebView) {
    fun dispatch(shortcut: Shortcut) {
        val downTime = SystemClock.uptimeMillis()
        webView.dispatchKeyEvent(KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, shortcut.keyCode, 0))
        webView.dispatchKeyEvent(KeyEvent(downTime, downTime, KeyEvent.ACTION_UP, shortcut.keyCode, 0))
    }
}
