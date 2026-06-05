package com.douyin.tv

import android.content.Context
import android.util.Log
import androidx.webkit.WebViewCompat

object WebViewSupport {
    private const val TAG = "DouyinTV"

    sealed interface Status {
        data class Ready(val versionName: String) : Status
        data object Missing : Status
    }

    fun check(context: Context, logVersion: Boolean = false): Status {
        val packageInfo = WebViewCompat.getCurrentWebViewPackage(context.applicationContext)
            ?: return Status.Missing
        val version = packageInfo.versionName ?: "unknown"
        if (logVersion) Log.d(TAG, "WebView version: $version")
        return Status.Ready(version)
    }
}
