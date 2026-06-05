package com.douyin.tv

import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.annotation.StringRes
import android.view.KeyEvent
import android.view.Window
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import com.douyin.tv.databinding.ActivityMainBinding

class MainActivity : ComponentActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var shortcutDispatcher: ShortcutDispatcher
    private lateinit var cursorController: CursorController
    private lateinit var remoteKeyController: RemoteKeyController

    private var menuVisible = false
    private var webViewConfigured = false
    private var originalWindowCallback: Window.Callback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        hideSystemUi()

        shortcutDispatcher = ShortcutDispatcher(binding.webView)
        cursorController = CursorController(binding.root, binding.webView, binding.cursorView, binding.mouseModeHint)
        remoteKeyController = RemoteKeyController(
            shortcutDispatcher = shortcutDispatcher,
            cursorController = cursorController,
            isMenuVisible = { menuVisible },
            showMenu = ::showMenu
        )

        setupMenuButtons()
        setupRetryOverlay()
        setupRemoteKeyCallback()
        setupBackCallback()
        startWebView()
        binding.webView.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        binding.webView.requestFocus()
    }

    override fun onDestroy() {
        originalWindowCallback?.let { window.callback = it }
        originalWindowCallback = null
        remoteKeyController.release()
        binding.webView.apply {
            stopLoading()
            webChromeClient = null
            webViewClient = WebViewClient()
            destroy()
        }
        super.onDestroy()
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun setupMenuButtons() {
        binding.btnMouseMode.setOnClickListener {
            hideMenu()
            cursorController.enter()
        }
        binding.btnPlayerFullscreen.setOnClickListener {
            hideMenu()
            binding.webView.post { shortcutDispatcher.dispatch(Shortcut.PlayerFullscreen) }
        }
        binding.btnWebImmersive.setOnClickListener {
            hideMenu()
            binding.webView.post { shortcutDispatcher.dispatch(Shortcut.WebImmersive) }
        }
        binding.btnPlayPause.setOnClickListener {
            hideMenu()
            binding.webView.post { shortcutDispatcher.dispatch(Shortcut.PlayPause) }
        }
        binding.btnRefresh.setOnClickListener {
            hideMenu()
            reloadHome()
        }
        binding.btnAutoplay.setOnClickListener {
            hideMenu()
            binding.webView.post { shortcutDispatcher.dispatch(Shortcut.Autoplay) }
        }
        binding.btnClearScreen.setOnClickListener {
            hideMenu()
            binding.webView.post { shortcutDispatcher.dispatch(Shortcut.ClearScreen) }
        }
        binding.btnMenuClose.setOnClickListener { hideMenu() }
    }

    private fun setupRetryOverlay() {
        binding.btnRetry.setOnClickListener { reloadHome() }
        binding.btnExitCancel.setOnClickListener { hideExitOverlay() }
        binding.btnExitConfirm.setOnClickListener { finish() }
    }

    private fun setupRemoteKeyCallback() {
        val callback = window.callback
        originalWindowCallback = callback
        window.callback = object : Window.Callback by callback {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (!binding.exitOverlay.isVisible && remoteKeyController.handle(event)) {
                    return true
                }
                return callback.dispatchKeyEvent(event)
            }
        }
    }

    private fun setupBackCallback() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBackAction()
                }
            }
        )
    }

    private fun handleBackAction() {
        when {
            binding.exitOverlay.isVisible -> hideExitOverlay()
            menuVisible -> hideMenu()
            cursorController.isActive -> cursorController.exit()
            binding.retryOverlay.isVisible -> showExitOverlay()
            binding.webView.canGoBack() -> binding.webView.goBack()
            else -> showExitOverlay()
        }
    }

    private fun showMenu() {
        hideExitOverlay()
        menuVisible = true
        binding.menuOverlay.isVisible = true
        binding.webView.clearFocus()
        binding.webView.isFocusable = false
        binding.webView.isFocusableInTouchMode = false
        binding.menuOverlay.requestFocus()
        binding.btnMouseMode.requestFocus()
    }

    private fun hideMenu() {
        menuVisible = false
        binding.menuOverlay.isVisible = false
        binding.webView.isFocusable = true
        binding.webView.isFocusableInTouchMode = true
        binding.webView.requestFocus()
    }

    private fun showExitOverlay() {
        binding.exitOverlay.isVisible = true
        binding.webView.clearFocus()
        binding.btnExitCancel.requestFocus()
    }

    private fun hideExitOverlay() {
        if (!binding.exitOverlay.isVisible) return
        binding.exitOverlay.isVisible = false
        binding.webView.requestFocus()
    }

    private fun startWebView() {
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        when (val status = WebViewSupport.check(this, logVersion = isDebuggable)) {
            WebViewSupport.Status.Missing -> showRetryOverlay(
                titleRes = R.string.webview_missing_title,
                messageRes = R.string.webview_missing_message
            )
            is WebViewSupport.Status.Ready -> {
                if (!webViewConfigured) {
                    WebViewConfigurator.configure(
                        webView = binding.webView,
                        isDebuggable = isDebuggable,
                        onMainFrameError = ::showPageLoadError
                    )
                    webViewConfigured = true
                } else {
                    binding.webView.loadUrl(WebViewConfigurator.DEFAULT_URL)
                }
                binding.retryOverlay.isVisible = false
            }
        }
    }

    private fun showPageLoadError() {
        showRetryOverlay()
    }

    private fun showRetryOverlay(
        @StringRes titleRes: Int = R.string.retry_title,
        @StringRes messageRes: Int = R.string.retry_message
    ) {
        hideExitOverlay()
        binding.retryTitle.setText(titleRes)
        binding.retryMessage.setText(messageRes)
        binding.retryOverlay.isVisible = true
        binding.btnRetry.requestFocus()
    }

    private fun reloadHome() {
        hideExitOverlay()
        binding.webView.isFocusable = true
        binding.webView.isFocusableInTouchMode = true
        startWebView()
        binding.webView.requestFocus()
    }
}
