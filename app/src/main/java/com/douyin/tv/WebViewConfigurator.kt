package com.douyin.tv

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient

object WebViewConfigurator {
    const val DEFAULT_URL = "https://www.douyin.com/?recommend=1&from_nav=1"

    @SuppressLint("SetJavaScriptEnabled")
    fun configure(webView: WebView, isDebuggable: Boolean, onMainFrameError: () -> Unit) {
        WebView.setWebContentsDebuggingEnabled(isDebuggable)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                userAgentString = USER_AGENT
                loadWithOverviewMode = true
                useWideViewPort = true
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                allowFileAccess = false
                allowContentAccess = false
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    injectScrollbarHideCss(view)
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    if (request.isForMainFrame) onMainFrameError()
                }
            }
            webChromeClient = WebChromeClient()
            loadUrl(DEFAULT_URL)
        }
    }

    private fun injectScrollbarHideCss(webView: WebView) {
        val css =
            "html,body{overflow:hidden!important;-ms-overflow-style:none;scrollbar-width:none;}" +
                "::-webkit-scrollbar{display:none;width:0;height:0;}"
        val js =
            "(function(){var s=document.createElement('style');s.textContent='" +
                JsEscaper.singleQuoted(css) +
                "';document.head.appendChild(s);})();"
        webView.evaluateJavascript(js, null)
    }

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36"
}
